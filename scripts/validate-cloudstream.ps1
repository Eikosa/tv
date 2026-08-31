param(
    [switch]$SkipBuild,
    [switch]$SkipNetwork,
    [switch]$CheckRemote,
    [int]$TimeoutSeconds = 18,
    [int]$Retries = 2
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

function Stop-Validation([string]$message) {
    Write-Error "VALIDATION FAILED: $message"
    exit 1
}

function Assert-Condition([bool]$condition, [string]$message) {
    if (-not $condition) { Stop-Validation $message }
}

function Assert-Test([bool]$condition, [string]$message) {
    if (-not $condition) { throw $message }
}

function Get-ResponseText($response) {
    if ($response.Content -is [byte[]]) { return [Text.Encoding]::UTF8.GetString($response.Content) }
    return [string]$response.Content
}

function Invoke-Retry([scriptblock]$operation, [string]$label) {
    $lastError = $null
    for ($attempt = 1; $attempt -le $Retries; $attempt++) {
        try { return & $operation } catch {
            $lastError = $_
            if ($attempt -lt $Retries) { Start-Sleep -Milliseconds (350 * $attempt) }
        }
    }
    throw "$label ($($lastError.Exception.Message))"
}

function Resolve-StreamUri([string]$baseUrl, [string]$relativeUrl) {
    return ([Uri]::new([Uri]$baseUrl, $relativeUrl)).AbsoluteUri
}

function Get-HttpRange([string]$url, [hashtable]$headers, [int]$lastByte = 1023) {
    Add-Type -AssemblyName System.Net.Http
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $true
    $client = [Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds($TimeoutSeconds)
    $request = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Get, $url)
    $request.Headers.Range = [Net.Http.Headers.RangeHeaderValue]::new(0, $lastByte)
    foreach ($key in $headers.Keys) {
        [void]$request.Headers.TryAddWithoutValidation([string]$key, [string]$headers[$key])
    }

    try {
        $response = $client.SendAsync($request, [Net.Http.HttpCompletionOption]::ResponseHeadersRead).GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "HTTP $([int]$response.StatusCode) $($response.ReasonPhrase)"
        }
        $stream = $response.Content.ReadAsStreamAsync().GetAwaiter().GetResult()
        try {
            $buffer = New-Object byte[] ($lastByte + 1)
            $bytesRead = $stream.Read($buffer, 0, $buffer.Length)
        } finally {
            $stream.Dispose()
        }
        return [pscustomobject]@{
            StatusCode = [int]$response.StatusCode
            BytesRead = $bytesRead
            ContentType = [string]$response.Content.Headers.ContentType
        }
    } finally {
        if ($response) { $response.Dispose() }
        $request.Dispose()
        $client.Dispose()
        $handler.Dispose()
    }
}

function Get-M3uEntries([string]$path) {
    $lines = Get-Content $path
    $entries = @()
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -notlike "#EXTINF*") { continue }
        $metadata = $lines[$i]
        $name = ($metadata -split ",")[-1].Trim()
        $id = [regex]::Match($metadata, 'tvg-id="([^"]+)"').Groups[1].Value
        $logo = [regex]::Match($metadata, 'tvg-logo="([^"]+)"').Groups[1].Value
        $group = [regex]::Match($metadata, 'group-title="([^"]+)"').Groups[1].Value
        $j = $i + 1
        while ($j -lt $lines.Count -and [string]::IsNullOrWhiteSpace($lines[$j])) { $j++ }
        Assert-Condition ($j -lt $lines.Count -and $lines[$j] -match '^https?://') "$name için doğrudan yayın URL'si yok."
        $entries += [pscustomobject]@{ Name = $name; Id = $id; Logo = $logo; Group = $group; Url = $lines[$j].Trim() }
    }
    return $entries
}

function Test-HlsChain($entry, [hashtable]$headers = @{}) {
    $playlistUrl = $entry.Url
    $response = Invoke-Retry {
        Invoke-WebRequest $playlistUrl -Headers $headers -TimeoutSec $TimeoutSeconds -MaximumRedirection 6 -UseBasicParsing
    } "$($entry.Name) ana HLS alınamadı"
    $playlist = Get-ResponseText $response
    Assert-Test ($response.StatusCode -in 200, 206 -and $playlist -match '#EXTM3U') "$($entry.Name) yanıtı geçerli HLS değil."

    if ($playlist -match '#EXT-X-STREAM-INF') {
        $lines = $playlist -split "`n" | ForEach-Object { $_.Trim() }
        $variants = @()
        for ($i = 0; $i -lt $lines.Count - 1; $i++) {
            if ($lines[$i] -like '#EXT-X-STREAM-INF*' -and $lines[$i + 1] -notlike '#*') {
                $bandwidth = [regex]::Match($lines[$i], 'BANDWIDTH=(\d+)').Groups[1].Value
                $variants += [pscustomobject]@{ Bandwidth = if ($bandwidth) { [int64]$bandwidth } else { 0 }; Url = $lines[$i + 1] }
            }
        }
        Assert-Test ($variants.Count -gt 0) "$($entry.Name) master listesinde kalite adresi yok."
        $variant = $variants | Sort-Object Bandwidth -Descending | Select-Object -First 1
        $playlistUrl = Resolve-StreamUri $playlistUrl $variant.Url
        $response = Invoke-Retry {
            Invoke-WebRequest $playlistUrl -Headers $headers -TimeoutSec $TimeoutSeconds -MaximumRedirection 6 -UseBasicParsing
        } "$($entry.Name) kalite HLS alınamadı"
        $playlist = Get-ResponseText $response
    }

    Assert-Test ($playlist -match '#EXTINF') "$($entry.Name) medya listesi video parçaları içermiyor."
    $mediaLines = @($playlist -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -and $_ -notlike '#*' })
    Assert-Test ($mediaLines.Count -gt 0) "$($entry.Name) medya parçası bulunamadı."
    $segment = Invoke-Retry {
        Get-HttpRange (Resolve-StreamUri $playlistUrl $mediaLines[-1]) $headers
    } "$($entry.Name) video parçası alınamadı"
    Assert-Test ($segment.StatusCode -in 200, 206 -and $segment.BytesRead -gt 0) "$($entry.Name) video parçası boş."
}

function Test-Logo([string]$url, [string]$name) {
    $response = Invoke-Retry {
        Invoke-WebRequest $url -Headers @{ "User-Agent" = "Mozilla/5.0" } -TimeoutSec $TimeoutSeconds -MaximumRedirection 6 -UseBasicParsing
    } "$name logosu alınamadı"
    $contentType = [string]$response.Headers["Content-Type"]
    Assert-Test ($response.StatusCode -in 200, 206 -and ($contentType -like 'image/*' -or $url -match '\.svg(?:\?|$)')) "$name logosu görsel döndürmüyor ($contentType)."
}

Write-Host "[1/8] CloudStream paketi derleniyor..."
if (-not $SkipBuild) {
    $taskGradleHome = Join-Path $repoRoot "..\gradle-home"
    $taskAndroidHome = Join-Path $repoRoot "..\android-sdk"
    $taskAndroidUserHome = Join-Path $repoRoot "..\android-user-home"
    if (-not (Test-Path $taskAndroidUserHome)) {
        New-Item -ItemType Directory -Path $taskAndroidUserHome -Force | Out-Null
    }
    if (Test-Path $taskGradleHome) { $env:GRADLE_USER_HOME = $taskGradleHome }
    $env:ANDROID_USER_HOME = $taskAndroidUserHome
    Remove-Item Env:ANDROID_SDK_HOME -ErrorAction SilentlyContinue
    if (Test-Path $taskAndroidHome) {
        $env:ANDROID_HOME = $taskAndroidHome
        $env:ANDROID_SDK_ROOT = $taskAndroidHome
    }
    & .\gradlew.bat TurkiyeTV:make makePluginsJson --no-daemon
    if ($LASTEXITCODE -ne 0) { Stop-Validation "Gradle derlemesi başarısız." }
}

$packagePath = Join-Path $repoRoot "TurkiyeTV\build\TurkiyeTV.cs3"
$generatedListPath = Join-Path $repoRoot "build\plugins.json"
$rootListPath = Join-Path $repoRoot "plugins.json"
$repoManifestPath = Join-Path $repoRoot "repo.json"
$sourcePath = Join-Path $repoRoot "TurkiyeTV\src\main\kotlin\com\eikosa\turkiyetv\TurkiyeTVProvider.kt"
$m3uPath = Join-Path $repoRoot "turkiye_ulusal_tv.m3u"
$buildConfig = Get-Content (Join-Path $repoRoot "TurkiyeTV\build.gradle.kts") -Raw
$versionMatch = [regex]::Match($buildConfig, '(?m)^version\s*=\s*(\d+)')
Assert-Condition $versionMatch.Success "TurkiyeTV sürümü build.gradle.kts içinde bulunamadı."
$expectedVersion = [int]$versionMatch.Groups[1].Value
Assert-Condition (Test-Path $packagePath) "TurkiyeTV.cs3 oluşturulmadı."
Assert-Condition (Test-Path $generatedListPath) "build/plugins.json oluşturulmadı."

Write-Host "[2/8] Paket, manifest ve yerel metadata doğrulanıyor..."
$packageBytes = [IO.File]::ReadAllBytes($packagePath)
$packageHash = ([BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash($packageBytes))).Replace("-", "").ToLowerInvariant()
$packageSize = $packageBytes.Length
$generatedEntry = ((Get-Content $generatedListPath -Raw) | ConvertFrom-Json)[0]
Assert-Condition ($generatedEntry.fileHash -eq "sha256-$packageHash") "build/plugins.json hash bilgisi paketle eşleşmiyor."
Assert-Condition ([int64]$generatedEntry.fileSize -eq $packageSize) "build/plugins.json boyutu paketle eşleşmiyor."
Assert-Condition ([int]$generatedEntry.version -eq $expectedVersion) "Plugin sürümü build.gradle.kts ile eşleşmiyor."
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($packagePath)
try {
    $manifestEntry = $archive.GetEntry("manifest.json")
    $dexEntry = $archive.GetEntry("classes.dex")
    Assert-Condition ($null -ne $manifestEntry) "Paket manifest.json içermiyor."
    Assert-Condition ($null -ne $dexEntry) "Paket classes.dex içermiyor."
    $reader = New-Object IO.StreamReader($manifestEntry.Open())
    try { $manifest = ($reader.ReadToEnd() | ConvertFrom-Json) } finally { $reader.Dispose() }
    Assert-Condition ($manifest.pluginClassName -eq "com.eikosa.turkiyetv.TurkiyeTVPlugin") "Plugin sınıfı manifest ile eşleşmiyor."
    Assert-Condition ([int]$manifest.version -eq $expectedVersion) "Manifest sürümü build.gradle.kts ile eşleşmiyor."
} finally { $archive.Dispose() }

Write-Host "[3/8] Repo adresleri, kanal kimlikleri ve kategoriler doğrulanıyor..."
$rootEntry = ((Get-Content $rootListPath -Raw) | ConvertFrom-Json)[0]
$repoInfo = Get-Content $repoManifestPath -Raw | ConvertFrom-Json
Assert-Condition ($rootEntry.internalName -eq "TurkiyeTV") "Ana plugins.json TurkiyeTV kaydını içermiyor."
Assert-Condition ($rootEntry.url -eq "https://raw.githubusercontent.com/Eikosa/tv/builds/TurkiyeTV.cs3") "Plugin URL'si builds dalını göstermiyor."
Assert-Condition ($repoInfo.pluginLists -contains "https://raw.githubusercontent.com/Eikosa/tv/builds/plugins.json") "repo.json builds plugin listesini göstermiyor."
$source = Get-Content $sourcePath -Raw
$entries = @(Get-M3uEntries $m3uPath)
Assert-Condition ($entries.Count -ge 190) "M3U kanal sayısı beklenenden az: $($entries.Count)."
Assert-Condition ($source -notmatch 'name\s*=\s*"\d+[.)]\s') "Kanal adlarında listeleme numarası bulundu."
Assert-Condition ($source -match 'newTvSeriesLoadResponse') "Resmî YouTube koleksiyon desteği bulunamadı."
$providerIds = @()
$providerIds += [regex]::Matches($source, '(?m)^\s*id\s*=\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
$providerIds += [regex]::Matches($source, 'tvGardenChannel\("([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
$providerIds += [regex]::Matches($source, 'youtubeChannel\("([^"]+)"') | ForEach-Object { "YouTube_$($_.Groups[1].Value)" }
$providerNames = @()
$providerNames += [regex]::Matches($source, '(?m)^\s*name\s*=\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
$providerNames += [regex]::Matches($source, 'tvGardenChannel\("[^"]+",\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
$providerNames += [regex]::Matches($source, 'youtubeChannel\("[^"]+",\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
Assert-Condition ($providerIds.Count -ge 220) "CloudStream katalog sayısı beklenenden az: $($providerIds.Count)."
Assert-Condition (@($providerIds | Group-Object | Where-Object Count -gt 1).Count -eq 0) "CloudStream içinde yinelenen kanal kimliği bulundu."
Assert-Condition (@($providerNames | Group-Object | Where-Object Count -gt 1).Count -eq 0) "CloudStream içinde yinelenen kanal adı bulundu."
$allowedGroups = @('Genel', 'Haber', 'Spor', 'Müzik', 'Çocuk', 'Eğitim', 'Belgesel', 'Kültür / Genel', 'Dini', 'Eğlence', 'Yaşam', 'İş / Dizi', 'Dizi / YouTube', 'Yerel', 'Bölgesel', 'Uluslararası', 'Anime / Animasyon')
$declaredGroups = @()
$declaredGroups += [regex]::Matches($source, '(?m)^\s*group\s*=\s*"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
$declaredGroups += [regex]::Matches($source, 'tvGardenChannel\([^\r\n]+?,\s*"([^"]+)",\s*\d+') | ForEach-Object { $_.Groups[1].Value }
$declaredGroups += [regex]::Matches($source, 'youtubeChannel\([^\r\n]+?,\s*"([^"]+)",\s*\d+') | ForEach-Object { $_.Groups[1].Value }
$unknownGroups = @($declaredGroups | Where-Object { $_ -notin $allowedGroups } | Select-Object -Unique)
Assert-Condition ($unknownGroups.Count -eq 0) "CloudStream ana sayfasında karşılığı olmayan kategori bulundu: $($unknownGroups -join ', ')."
foreach ($property in 'Name', 'Id', 'Url') {
    $duplicates = @($entries | Group-Object $property | Where-Object { $_.Count -gt 1 })
    Assert-Condition ($duplicates.Count -eq 0) "M3U içinde yinelenen $property bulundu: $($duplicates.Name -join ', ')."
}
foreach ($entry in $entries) {
    Assert-Condition (-not [string]::IsNullOrWhiteSpace($entry.Id)) "$($entry.Name) tvg-id içermiyor."
    Assert-Condition (-not [string]::IsNullOrWhiteSpace($entry.Group)) "$($entry.Name) kategori içermiyor."
    Assert-Condition (-not [string]::IsNullOrWhiteSpace($entry.Logo)) "$($entry.Name) logo içermiyor."
    Assert-Condition ($entry.Url -notmatch 'youtube\.com|youtu\.be') "$($entry.Name) M3U içinde oynatılamayan YouTube sayfası kullanıyor."
}

if (-not $SkipNetwork) {
    Write-Host "[4/8] $($entries.Count) M3U yayını master -> kalite -> video parçası zinciriyle test ediliyor..."
    $streamHeaders = @{ "User-Agent" = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/153.0.0.0 Safari/537.36" }
    $index = 0
    $streamFailures = @()
    foreach ($entry in $entries) {
        $index++
        try {
            Test-HlsChain $entry $streamHeaders
            Write-Host "  OK [$index/$($entries.Count)] $($entry.Name)"
        } catch {
            $streamFailures += "$($entry.Name): $($_.Exception.Message)"
            Write-Warning "FAIL [$index/$($entries.Count)] $($entry.Name)"
        }
    }
    Assert-Condition ($streamFailures.Count -eq 0) "Çalışmayan HLS yayınları:`n$($streamFailures -join "`n")"

    Write-Host "[5/8] Benzersiz kanal logoları test ediliyor..."
    $logos = $entries | Group-Object Logo | ForEach-Object { $_.Group[0] }
    $logoFailures = @()
    foreach ($entry in $logos) {
        try { Test-Logo $entry.Logo $entry.Name } catch { $logoFailures += "$($entry.Name): $($_.Exception.Message)" }
    }
    $collectionLogos = [regex]::Matches($source, 'YouTubeCollection\(\s*id\s*=\s*"[^"]+"[\s\S]*?logoUrl\s*=\s*"([^"]+)"[\s\S]*?\)') |
        ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
    foreach ($logo in $collectionLogos) {
        try { Test-Logo $logo "Resmî YouTube koleksiyonu" } catch { $logoFailures += "Resmî YouTube koleksiyonu: $($_.Exception.Message)" }
    }
    Assert-Condition ($logoFailures.Count -eq 0) "Çalışmayan logolar:`n$($logoFailures -join "`n")"
    Write-Host "  OK $($logos.Count + $collectionLogos.Count) logo"

    Write-Host "[6/8] YouTube yayınları ve resmî kanal RSS'leri test ediliyor..."
    $youtubeHeaders = @{ "User-Agent" = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/153.0.0.0 Safari/537.36"; "Accept-Language" = "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7" }
    $youtubeIds = [regex]::Matches($source, 'youtubeChannel\("[^"]+",\s*"[^"]+",\s*"([A-Za-z0-9_-]{11})"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
    Assert-Condition ($youtubeIds.Count -gt 0) "YouTube yayın kimliği bulunamadı."
    $keyPage = Invoke-Retry { Invoke-WebRequest "https://www.youtube.com/watch?v=$($youtubeIds[0])" -Headers $youtubeHeaders -TimeoutSec $TimeoutSeconds -UseBasicParsing } "YouTube sayfası alınamadı"
    $apiKeyMatch = [regex]::Match((Get-ResponseText $keyPage), '"INNERTUBE_API_KEY"\s*:\s*"([^"]+)"')
    Assert-Condition $apiKeyMatch.Success "YouTube player API anahtarı alınamadı."
    $apiKey = $apiKeyMatch.Groups[1].Value
    $youtubeFailures = @()
    foreach ($youtubeId in $youtubeIds) {
        try {
            $body = @{ context = @{ client = @{ hl = "tr"; gl = "TR"; clientName = "ANDROID"; clientVersion = "20.10.38" } }; videoId = $youtubeId; playbackContext = @{ contentPlaybackContext = @{ html5Preference = "HTML5_PREF_WANTS" } } } | ConvertTo-Json -Depth 10
            $playerResponse = Invoke-Retry { Invoke-WebRequest "https://www.youtube.com/youtubei/v1/player?key=$apiKey" -Method Post -Headers @{ "User-Agent" = "com.google.android.youtube/20.10.38 (Linux; U; Android 14)"; "Accept-Language" = "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7" } -ContentType "application/json" -Body $body -TimeoutSec $TimeoutSeconds -UseBasicParsing } "YouTube $youtubeId player yanıtı alınamadı"
            $player = (Get-ResponseText $playerResponse) | ConvertFrom-Json
            $hasPlayableData = -not [string]::IsNullOrWhiteSpace($player.streamingData.hlsManifestUrl) -or @($player.streamingData.formats).Count -gt 0 -or @($player.streamingData.adaptiveFormats).Count -gt 0
            Assert-Test ($player.playabilityStatus.status -eq 'OK' -and $hasPlayableData) "YouTube $youtubeId oynatılamıyor ($($player.playabilityStatus.status): $($player.playabilityStatus.reason))."
            Write-Host "  OK YouTube $youtubeId"
        } catch { $youtubeFailures += "$youtubeId`: $($_.Exception.Message)" }
    }
    $collectionIds = [regex]::Matches($source, 'channelId\s*=\s*"([A-Za-z0-9_-]+)"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
    foreach ($channelId in $collectionIds) {
        try {
            $feed = Invoke-Retry { Invoke-WebRequest "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId" -Headers $youtubeHeaders -TimeoutSec $TimeoutSeconds -UseBasicParsing } "YouTube RSS $channelId alınamadı"
            $feedText = Get-ResponseText $feed
            Assert-Test ($feedText -match '<yt:channelId>' -and $feedText -match '<entry>') "YouTube RSS $channelId video içermiyor."
            Write-Host "  OK RSS $channelId"
        } catch { $youtubeFailures += "$channelId`: $($_.Exception.Message)" }
    }
    Assert-Condition ($youtubeFailures.Count -eq 0) "Çalışmayan YouTube kaynakları:`n$($youtubeFailures -join "`n")"

    Write-Host "[7/8] EPG ve NOW TV dinamik çözümü test ediliyor..."
    $epg = Invoke-Retry { Invoke-WebRequest "https://iptv-epg.org/files/epg-tr.xml" -TimeoutSec ($TimeoutSeconds * 2) -UseBasicParsing } "EPG alınamadı"
    $epgIds = [regex]::Matches((Get-ResponseText $epg), '<channel\s+id="([^"]+)"') | ForEach-Object { $_.Groups[1].Value }
    Assert-Condition ($epgIds.Count -gt 0) "EPG kanal kimliği içermiyor."
    $epgSet = @{}; foreach ($id in $epgIds) { $epgSet[$id] = $true }
    $epgMatches = @($entries | Where-Object { $epgSet.ContainsKey($_.Id) }).Count
    Assert-Condition ($epgMatches -ge 20) "EPG eşleşmesi beklenenden az: $epgMatches/$($entries.Count)."
    Write-Host "  OK EPG $epgMatches/$($entries.Count) doğrudan kimlik eşleşmesi"
    $nowPage = Invoke-Retry { Invoke-WebRequest "https://www.nowtv.com.tr/canli-yayin" -Headers $youtubeHeaders -TimeoutSec $TimeoutSeconds -UseBasicParsing } "NOW TV sayfası alınamadı"
    $nowUrl = [regex]::Matches((Get-ResponseText $nowPage), 'https?://[^"''\s]+\.m3u8[^"''\s]*') | ForEach-Object { $_.Value.Replace("&amp;", "&").Replace("\u0026", "&") } | Where-Object { $_ -like "*nowtv.daioncdn.net/nowtv/nowtv.m3u8*" } | Select-Object -First 1
    Assert-Condition (-not [string]::IsNullOrWhiteSpace($nowUrl) -and $nowUrl -match '[?&]st=' -and $nowUrl -match '[?&]e=') "NOW TV taze imzalı yayın adresi üretmedi."
    Test-HlsChain ([pscustomobject]@{ Name = "NOW TV dinamik"; Url = $nowUrl }) ($streamHeaders + @{ "Origin" = "https://www.nowtv.com.tr"; "Referer" = "https://www.nowtv.com.tr/" })
    Write-Host "  OK NOW TV dinamik yayın"
} else {
    Write-Host "[4-7/8] Ağ kontrolleri atlandı (-SkipNetwork)."
}

if ($CheckRemote) {
    Write-Host "[8/8] GitHub builds dalı doğrulanıyor..."
    $nonce = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $remoteMeta = ((Invoke-WebRequest "https://raw.githubusercontent.com/Eikosa/tv/main/plugins.json?check=$nonce" -UseBasicParsing).Content | ConvertFrom-Json)[0]
    $remoteResponse = Invoke-WebRequest "$($remoteMeta.url)?check=$nonce" -UseBasicParsing
    $memory = New-Object IO.MemoryStream
    $remoteResponse.RawContentStream.CopyTo($memory)
    $remoteData = $memory.ToArray()
    $remoteHash = ([BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash($remoteData))).Replace("-", "").ToLowerInvariant()
    Assert-Condition ($remoteHash -eq $remoteMeta.fileHash.Replace("sha256-", "")) "GitHub paketi ile metadata hash'i eşleşmiyor."
    Assert-Condition ($remoteData.Length -eq [int64]$remoteMeta.fileSize) "GitHub paketi ile metadata boyutu eşleşmiyor."
    Assert-Condition ([int]$remoteMeta.version -eq $expectedVersion) "GitHub plugin sürümü beklenen $expectedVersion değil."
} else { Write-Host "[8/8] Uzak GitHub kontrolü atlandı (-CheckRemote verilmedi)." }

Write-Host "VALIDATION PASSED: Paket, katalog, HLS parçaları, logolar, YouTube, EPG ve özel çözücüler başarılı."

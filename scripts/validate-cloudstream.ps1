param(
    [switch]$SkipBuild,
    [switch]$SkipNetwork,
    [switch]$CheckRemote
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

function Stop-Validation([string]$message) {
    Write-Error "VALIDATION FAILED: $message"
    exit 1
}

function Assert-Condition([bool]$condition, [string]$message) {
    if (-not $condition) { Stop-Validation $message }
}

Write-Host "[1/6] CloudStream paketi derleniyor..."
if (-not $SkipBuild) {
    $taskGradleHome = Join-Path $repoRoot "..\gradle-home"
    $taskAndroidHome = Join-Path $repoRoot "..\android-sdk"
    if (Test-Path $taskGradleHome) { $env:GRADLE_USER_HOME = $taskGradleHome }
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

Assert-Condition (Test-Path $packagePath) "TurkiyeTV.cs3 oluşturulmadı."
Assert-Condition (Test-Path $generatedListPath) "build/plugins.json oluşturulmadı."

Write-Host "[2/6] Paket, manifest ve yerel metadata doğrulanıyor..."
$packageBytes = [IO.File]::ReadAllBytes($packagePath)
$packageHash = ([BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash($packageBytes))).Replace("-", "").ToLowerInvariant()
$packageSize = $packageBytes.Length
$generatedEntry = ((Get-Content $generatedListPath -Raw) | ConvertFrom-Json)[0]
Assert-Condition ($generatedEntry.fileHash -eq "sha256-$packageHash") "build/plugins.json hash bilgisi paketle eşleşmiyor."
Assert-Condition ([int64]$generatedEntry.fileSize -eq $packageSize) "build/plugins.json boyutu paketle eşleşmiyor."
Assert-Condition ([int]$generatedEntry.version -eq 4) "Beklenmeyen plugin sürümü: $($generatedEntry.version)."

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($packagePath)
try {
    $manifestEntry = $archive.GetEntry("manifest.json")
    $dexEntry = $archive.GetEntry("classes.dex")
    Assert-Condition ($null -ne $manifestEntry) "Paket manifest.json içermiyor."
    Assert-Condition ($null -ne $dexEntry) "Paket classes.dex içermiyor."
    $manifestReader = New-Object IO.StreamReader($manifestEntry.Open())
    try { $manifest = ($manifestReader.ReadToEnd() | ConvertFrom-Json) } finally { $manifestReader.Dispose() }
    Assert-Condition ($manifest.pluginClassName -eq "com.eikosa.turkiyetv.TurkiyeTVPlugin") "Plugin sınıfı manifest ile eşleşmiyor."
    Assert-Condition ([int]$manifest.version -eq [int]$generatedEntry.version) "Manifest ve plugin listesi sürümü eşleşmiyor."
} finally {
    $archive.Dispose()
}

Write-Host "[3/6] Repo adresleri ve metadata doğrulanıyor..."
$rootEntry = ((Get-Content $rootListPath -Raw) | ConvertFrom-Json)[0]
$repoInfo = Get-Content $repoManifestPath -Raw | ConvertFrom-Json
Assert-Condition ($rootEntry.internalName -eq "TurkiyeTV") "Ana plugins.json TurkiyeTV kaydını içermiyor."
Assert-Condition ($rootEntry.url -eq "https://raw.githubusercontent.com/Eikosa/tv/builds/TurkiyeTV.cs3") "Plugin URL'si builds dalını göstermiyor."
Assert-Condition ($repoInfo.pluginLists -contains "https://raw.githubusercontent.com/Eikosa/tv/builds/plugins.json") "repo.json builds plugin listesini göstermiyor."

Write-Host "[4/6] Kanal isimleri ve kaynak kuralları doğrulanıyor..."
$source = Get-Content $sourcePath -Raw
Assert-Condition ($source -notmatch 'name\s*=\s*"\$\{channelNumber\}\\?\.') "Kanal adlarında listeleme numarası üreten ifade bulundu."
Assert-Condition ($source -match 'group\s*=\s*"Haber"') "Kanal kategori verisi bulunamadı."
Assert-Condition ($source -match 'isYouTubeUrl') "YouTube URL yönlendirmesi bulunamadı."

if (-not $SkipNetwork) {
    Write-Host "[5/6] YouTube canlı yayınları gerçek HLS üretimiyle test ediliyor..."
    $youtubeHeaders = @{
        "User-Agent" = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36"
        "Accept-Language" = "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7"
    }
    $youtubeIds = [regex]::Matches($source, 'youtubeChannel\("[^"]+",\s*"[^"]+",\s*"([A-Za-z0-9_-]{11})"') |
        ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
    Assert-Condition ($youtubeIds.Count -gt 0) "YouTube kanal kimliği bulunamadı."

    $apiKeyPage = Invoke-WebRequest "https://www.youtube.com/watch?v=$($youtubeIds[0])" -Headers $youtubeHeaders -UseBasicParsing
    $apiKeyMatch = [regex]::Match($apiKeyPage.Content, '"INNERTUBE_API_KEY"\s*:\s*"([^"]+)"')
    Assert-Condition $apiKeyMatch.Success "YouTube player API anahtarı alınamadı."
    $apiKey = $apiKeyMatch.Groups[1].Value

    foreach ($youtubeId in $youtubeIds) {
        $requestBody = @{
            context = @{ client = @{ hl = "tr"; gl = "TR"; clientName = "ANDROID"; clientVersion = "20.10.38" } }
            videoId = $youtubeId
            playbackContext = @{ contentPlaybackContext = @{ html5Preference = "HTML5_PREF_WANTS" } }
        } | ConvertTo-Json -Depth 10
        $player = Invoke-WebRequest "https://www.youtube.com/youtubei/v1/player?key=$apiKey" -Method Post -Headers @{
            "User-Agent" = "com.google.android.youtube/20.10.38 (Linux; U; Android 14)"
            "Accept-Language" = "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7"
        } -ContentType "application/json" -Body $requestBody -UseBasicParsing
        $playerJson = $player.Content | ConvertFrom-Json
        $hlsUrl = $playerJson.streamingData.hlsManifestUrl
        Assert-Condition (-not [string]::IsNullOrWhiteSpace($hlsUrl)) "YouTube $youtubeId HLS üretmedi ($($playerJson.playabilityStatus.status): $($playerJson.playabilityStatus.reason))."
        Write-Host "  OK $youtubeId"
    }
} else {
    Write-Host "[5/6] Ağ kontrolleri atlandı (-SkipNetwork)."
}

if ($CheckRemote) {
    Write-Host "[6/6] GitHub builds dalı doğrulanıyor..."
    $remoteMeta = ((Invoke-WebRequest "https://raw.githubusercontent.com/Eikosa/tv/main/plugins.json?check=$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())" -UseBasicParsing).Content | ConvertFrom-Json)[0]
    $remoteResponse = Invoke-WebRequest "$($remoteMeta.url)?check=$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())" -UseBasicParsing
    $remoteBytes = $remoteResponse.RawContentStream
    $remoteMemory = New-Object IO.MemoryStream
    $remoteBytes.CopyTo($remoteMemory)
    $remoteData = $remoteMemory.ToArray()
    $remoteHash = ([BitConverter]::ToString([Security.Cryptography.SHA256]::Create().ComputeHash($remoteData))).Replace("-", "").ToLowerInvariant()
    Assert-Condition ($remoteHash -eq $remoteMeta.fileHash.Replace("sha256-", "")) "GitHub paketi ile metadata hash'i eşleşmiyor."
    Assert-Condition ($remoteData.Length -eq [int64]$remoteMeta.fileSize) "GitHub paketi ile metadata boyutu eşleşmiyor."
    Assert-Condition ([int]$remoteMeta.version -eq 4) "GitHub'daki plugin sürümü beklenenden farklı."
} else {
    Write-Host "[6/6] Uzak GitHub kontrolü atlandı (-CheckRemote verilmedi)."
}

Write-Host "VALIDATION PASSED: Push için paket ve yayın kontrolleri başarılı."

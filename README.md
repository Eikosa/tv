# Türkiye Türkçe Canlı TV — CloudStream ve M3U kullanım rehberi

Bu depo, herkese açık Türkçe/Türkiye canlı HLS yayınlarını ve doğrulanmış YouTube içeriklerini düzenli bir katalogda toplar. CloudStream sağlayıcısında **220 canlı yayın ve 2 resmî video koleksiyonu**, statik M3U listesinde ise **190 doğrudan HLS yayını** vardır. Popüler ulusal ve haber kanalları önde; daha az kullanılan müzik, eğitim, bölgesel ve yerel kanallar daha sonra gelir. Kanal adlarının başında sıra numarası gösterilmez ve her yayın yalnızca tek bir kategoride yer alır.

CloudStream/Kotlin kataloğu ile M3U iki ayrı çıktıdır. Aynı kanalı ikisinde de görmek normaldir: CloudStream özel çözücü, arama ve video koleksiyonu özelliklerini kullanabilir; M3U ise VLC, Kodi, IPTV uygulamaları ve medya sunucuları için yalnızca doğrudan oynatılabilir akışları içerir.

## Hızlı bağlantılar

> [!IMPORTANT]
> **Doğru M3U Adresi:** IPTV oynatıcılara yalnızca `https://raw.githubusercontent.com/Eikosa/tv/` yazıldığında dosya adı belirtilmediği için GitHub 404 (Not Found) hatası verir ve kanallar yüklenmez. Oynatıcılarınıza aşağıdaki tam M3U bağlantılarından birini ekleyiniz:
> - **M3U Playlist (Ana):** `https://raw.githubusercontent.com/Eikosa/tv/main/turkiye_ulusal_tv.m3u`
> - **M3U Playlist (Kısa):** `https://raw.githubusercontent.com/Eikosa/tv/main/playlist.m3u`
> - **EPG / XMLTV Rehberi:** `https://iptv-epg.org/files/epg-tr.xml`
> - **CloudStream Depo Adresi (repo):** `https://raw.githubusercontent.com/Eikosa/tv/main/repo.json`
> - **CloudStream Eklenti Paketi (.cs3):** `https://raw.githubusercontent.com/Eikosa/tv/builds/TurkiyeTV.cs3`

## CloudStream kullanımı

1. CloudStream'i açıp eklenti/depo yönetimine girin.
2. Yukarıdaki `main/repo.json` adresini depo adresi olarak ekleyin. `plugins.json` doğrudan plugin listesidir; depo adresi olarak `repo.json` kullanılmalıdır.
3. `TurkiyeTV` eklentisini kurun veya güncelleyin.
4. Canlı içerikler bölümünden kanalı seçin; arama kutusuyla kanal adına göre arayın.

GitHub Actions, `main` dalındaki değişikliklerden sonra `builds` dalında `.cs3` ve `plugins.json` dosyalarını otomatik üretir. İlk kurulumda workflow'un tamamlanmasını bekleyin; sonrasında depo adresini ekleyin. Eklentiyi doğrudan `.cs3` dosyasını indirerek elle kurmak da mümkündür.

Teve2'nin yayıncı tarafından sunulan güncel master HLS adresi hem CloudStream sağlayıcısında hem M3U listesinde bulunur. CloudStream gerekli yönlendirme başlıklarını oynatma sırasında ekler; M3U tarafı da doğrulanmış genel master adresini kullanır.

CloudStream sağlayıcısında YouTube yayınları önce canlı HLS olarak, bu mümkün değilse CloudStream'in yerleşik çözücüsüyle açılır. CNN TÜRK, SÖZCÜ TV, Bloomberg HT, Cartoon Network içerikleri ve doğrulanmış dizi/film yayınları bu yolla sunulur. Discovery Channel Türkiye ile National Geographic Türkiye, ücretli TV yayınlarının yetkisiz kopyaları yerine yayıncıların **resmî Türkçe YouTube kanallarındaki güncel videoları** gösteren koleksiyonlardır. YouTube izleme sayfaları doğrudan M3U'ya yazılmaz; standart M3U oynatıcılar bu sayfaları medya akışı olarak açamaz.

## M3U destekleyen IPTV uygulamaları

`turkiye_ulusal_tv.m3u` listesini URL olarak veya dosya indirerek kullanabilirsiniz. M3U URL'sini uygulamanın playlist/URL alanına yapıştırın; dosya yönteminde indirdiğiniz `.m3u` dosyasını yerel playlist olarak seçin.

Android TV/telefon için TiviMate, Televizo, OTT Navigator, IPTV Smarters, SmartOne ve benzeri M3U oynatıcılar; bilgisayar için VLC, mpv ve benzeri oynatıcılar kullanılabilir. Uygulamanın EPG/XMLTV alanı varsa şu adresi ekleyin: `https://iptv-epg.org/files/epg-tr.xml`.

Playlist'teki `group-title` kanal kategorisini, `tvg-logo` kanal görselini, `tvg-id` ise EPG eşleşmesini belirtir. Uygulama EPG desteklemiyorsa kanallar yine izlenebilir; yalnızca program rehberi görünmez.

## VLC

VLC'de **Medya → Ağ akışı aç** seçeneğine M3U URL'sini girin. Playlist açılmazsa M3U dosyasını indirip VLC'ye sürükleyin veya tek bir `.m3u8` adresini aynı ekrandan açın.

## Kodi

1. Kodi'ye **PVR IPTV Simple Client** eklentisini kurun.
2. Playlist türünü **uzak yol** seçip M3U URL'sini girin.
3. EPG/XMLTV alanına EPG adresini girin.
4. Kodi'yi yeniden başlatıp TV bölümünü açın.

## Komut satırı, medya sunucusu ve otomasyon

Playlist'i indirme:

```text
curl -L "https://raw.githubusercontent.com/Eikosa/tv/main/turkiye_ulusal_tv.m3u" -o turkiye.m3u
```

Tek bir akışı `ffplay` veya `mpv` ile açma:

```text
ffplay "https://tv-trt1.medya.trt.com.tr/master.m3u8"
mpv "https://tv-trt1.medya.trt.com.tr/master.m3u8"
```

Ev sunucusu, NAS veya medya merkezi kullanıyorsanız listeyi belirli aralıklarla yeniden indirip yerel ağınızdaki uygulamalara sunabilirsiniz. Kendi uygulamanızda M3U satırlarını ayrıştırarak kanal adı, kategori, logo, EPG kimliği ve yayın URL'si alanlarını kataloglayabilirsiniz. Bu yöntem web arayüzü, mobil uygulama, bot, favori kanal listesi veya kanal arama servisi oluşturmak için kullanılabilir.

## Geliştirici ve bakım

Windows'ta kaynak projeyi derlemek için:

```text
gradlew.bat TurkiyeTV:make makePluginsJson
```

Yeni kaynak eklerken HTTPS, doğrudan HLS URL'si, çalışan bir logo ve mümkünse doğru EPG `tvg-id` kullanın. Göndermeden önce tam denetimi çalıştırın:

```text
powershell -ExecutionPolicy Bypass -File scripts/validate-cloudstream.ps1
```

Doğrulayıcı paketi derler; manifest, sürüm ve SHA-256 bilgisini karşılaştırır; M3U'daki yinelenen ad/kimlik/adresleri bulur; her HLS için ana liste → en yüksek kalite → gerçek video parçası zincirini indirir; logoları, YouTube oynatılabilirliğini, resmî kanal RSS'lerini, EPG eşleşmelerini ve NOW TV'nin taze imzalı adresini kontrol eder. Ağ testi özellikle atlanmak istenirse `-SkipNetwork`, yayımlanmış GitHub paketini de doğrulamak için `-CheckRemote` kullanılabilir.

## Kullanım ve yasal not

Bu depo yalnızca herkese açık yayın adreslerini listeler; üyelik, kimlik doğrulama veya ücretli servislere erişim sağlamaz. Yayınların kullanım hakkı, bölgesel kısıtları ve telif koşulları yayıncıya göre değişebilir. Yalnızca yetkili olduğunuz ve yasal kullanım hakkınız bulunan içerikleri izleyin. Akışlar yayıncılar tarafından değiştirilebildiği için çalışma durumu zaman içinde farklılaşabilir.

Kaynak listeler: [iptv-org Türkiye listesi](https://iptv-org.github.io/iptv/countries/tr.m3u) ve [feroxx/canlitv.m3u](https://raw.githubusercontent.com/feroxx/test/refs/heads/main/Kanallar/canlitv.m3u).

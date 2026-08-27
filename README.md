# Türkiye Türkçe Canlı TV — CloudStream ve M3U kullanım rehberi

Bu depo, herkese açık Türkçe/Türkiye canlı HLS yayınlarını tek bir listede toplar. Şu an 98 kanal içerir. Popüler ulusal ve haber kanalları listede önde; daha az kullanılan müzik, eğitim ve yerel kanallar daha sonra gelir.

## Hızlı bağlantılar

- M3U: `https://raw.githubusercontent.com/Eikosa/tv/main/turkiye_ulusal_tv.m3u`
- EPG/XMLTV: `https://iptv-epg.org/files/epg-tr.xml`
- CloudStream depo indeksi (önerilen): `https://raw.githubusercontent.com/Eikosa/tv/main/plugins.json`
- Güncel otomatik derleme indeksi: `https://raw.githubusercontent.com/Eikosa/tv/builds/plugins.json`
- CloudStream eklentisi: `https://raw.githubusercontent.com/Eikosa/tv/builds/TurkiyeTV.cs3`

## CloudStream kullanımı

1. CloudStream'i açıp eklenti/depo yönetimine girin.
2. Yukarıdaki `main/plugins.json` adresini depo adresi olarak ekleyin. GitHub önbelleği nedeniyle sorun yaşarsanız `builds/plugins.json` adresini deneyin.
3. `TurkiyeTV` eklentisini kurun veya güncelleyin.
4. Canlı içerikler bölümünden kanalı seçin; arama kutusuyla kanal adına göre arayın.

GitHub Actions, `main` dalındaki değişikliklerden sonra `builds` dalında `.cs3` ve `plugins.json` dosyalarını otomatik üretir. İlk kurulumda workflow'un tamamlanmasını bekleyin; sonrasında depo adresini ekleyin. Eklentiyi doğrudan `.cs3` dosyasını indirerek elle kurmak da mümkündür.

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

Yeni kaynak eklerken HTTPS, doğrudan `.m3u8` HLS URL'si, çalışan bir logo ve mümkünse doğru EPG `tvg-id` kullanın. Yayın kontrolünde yalnızca HTTP durum koduna güvenmeyin; yanıtın `#EXTM3U` veya `#EXT-X-` playlist içeriği verdiğini de kontrol edin. Logo adresi HTTP 2xx ve görsel içerik döndürmelidir. Kanal sıralaması `TurkiyeTVProvider.kt` ve M3U dosyasında aynı tutulmalıdır.

## Kullanım ve yasal not

Bu depo yalnızca herkese açık yayın adreslerini listeler; üyelik, kimlik doğrulama veya ücretli servislere erişim sağlamaz. Yayınların kullanım hakkı, bölgesel kısıtları ve telif koşulları yayıncıya göre değişebilir. Yalnızca yetkili olduğunuz ve yasal kullanım hakkınız bulunan içerikleri izleyin. Akışlar yayıncılar tarafından değiştirilebildiği için çalışma durumu zaman içinde farklılaşabilir.

Kaynak listeler: [iptv-org Türkiye listesi](https://iptv-org.github.io/iptv/countries/tr.m3u) ve [feroxx/canlitv.m3u](https://raw.githubusercontent.com/feroxx/test/refs/heads/main/Kanallar/canlitv.m3u).

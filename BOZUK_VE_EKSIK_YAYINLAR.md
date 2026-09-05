# Bozuk, Kapanmış ve Eksik Yayınlar Takip Listesi

Bu dosya; depodan geçici veya kalıcı olarak kaldırılan, sunucusu kapanan, token/koruma nedeniyle doğrudan açılamayan veya yalnızca YouTube üzerinden sunulabilen yayınları ve eski adreslerini içerir. İleride yeni çalışan bir HLS adresi veya çözümleyici bulunduğunda tekrar kataloğa ve M3U'ya eklenmek üzere takip amacıyla tutulmaktadır.

---

## 1. Sunucusu Kapanan veya Kaldırılan Yayınlar

| Kanal Adı | Eski / Denenen URL | Karşılaşılan Hata | Notlar & Alternatif Durumu |
| :--- | :--- | :--- | :--- |
| **Finans Türk TV** | `https://yayin30.haber100.com/live/finansturk/playlist.m3u8` | `HTTP 404 Not Found` | Haber100 CDN yayını durdurdu. Yerine ulusal haber kanalı **Meclis TV** eklendi. |
| **Satranç TV** | `http://139.162.182.79/live/test/index.m3u8` | `HTTP 404 Not Found` | Geçici IP üzerindeki test sunucusu kapandı. Yerine **Red Bull TV** eklendi. |
| **Kanal 34** | `https://live.euromediacenter.com/kanal34/tracks-v1a1/playlist.m3u8` | Master 200, `.ts` segmentleri `404` | Playlist güncelleniyor ancak parçalar sunucuda bulunamıyor. Yerine **MTürk TV** eklendi. |
| **TGRT Belgesel** | `https://b01c02nl.mediatriple.net/videoonlylive/mtsxxkzwwuqtglive/broadcast_5fe462afc6a0e.smil/playlist.m3u8` | Alt `chunklist` 404 | Mediatriple üzerindeki eski smil bağlantısı düşmüş. Güncel HLS aranıyor. |
| **TRT 3 (live.trt CDN)** | `https://tv-trt3.live.trt.com.tr/master.m3u8` | `master_720.m3u8` 404 | TRT'nin `live.trt.com.tr` alan adı eski; güncel TBMM/Meclis yayını Ercdn (`meclistv-live.ercdn.net`) üzerinden eklendi. |
| **Kadırga TV** | `https://edge1.taksimbilisim.com/kadirgatv/bant1/playlist.m3u8` | `HTTP 404 Not Found` | Taksim Bilişim ve Artı Dijital sunucuları kapalı. |
| **EuroStar** | `https://dogus-live.daioncdn.net/eurostar/eurostar.m3u8` | `HTTP 403 Forbidden` / 404 | Doğuş Grubu doğrudan genel erişimi kısıtlamış durumda. |
| **Vatan TV** | `https://live.artidijitalmedya.com/artidijital_vatantv/vatantv/playlist.m3u8` | `HTTP 404 Not Found` | Artı Dijital sunucusunda yayın pasif. |
| **Köy TV** | `https://live.artidijitalmedya.com/artidijital_koytv/koytv/playlist.m3u8` | `HTTP 404 Not Found` | Sunucu kapalı. |

---

## 2. Doğrudan HLS Akışı Eksik Olan (Yalnızca YouTube'da Çalışan) Popüler Kanallar

Aşağıdaki kanallar CloudStream üzerinde YouTube çözücüsüyle canlı olarak izlenebilmektedir; ancak statik M3U oynatıcılar (VLC, IPTV Smarters vb.) için doğrudan kalıcı bir `.m3u8` bağlantısı aranmaktadır:

| Kanal Adı | Mevcut YouTube Canlı ID | Durum / Aranacak Nokta |
| :--- | :--- | :--- |
| **CNN TÜRK** | `6N8_r2uwLEc` | CloudStream'de YouTube HLS olarak çalışıyor. Kalıcı doğrudan genel master HLS aranıyor. |
| **SÖZCÜ TV** | `ztmY_cCtUl0` | CloudStream'de YouTube HLS olarak çalışıyor. Doğrudan HLS akışı aranıyor. |
| **TV 100** | `4WSvLRk83-c` | DaionCDN akışları kısa süreli oturum parametreli (`sid=...`). Statik HLS bulunursa M3U'ya eklenecek. |
| **KRT TV** | `_k0wG2Qah1g` | YouTube canlı yayını aktif; doğrudan CDN akışı aranıyor. |
| **A Para** | `Fas1VhgP8Uk` | TurkNet/ercdn üzerindeki `apara/apara.m3u8` 500 hatası dönüyor. |
| **A News** | - | TurkNet/ercdn üzerindeki `anews/anews.m3u8` 502 Bad Gateway dönüyor. |
| **Ulusal Kanal** | `Gcxkjxhbhk8` | YouTube canlı yayını aktif. |
| **Bengütürk TV** | `MOhcWsOL1Us` | YouTube canlı yayını aktif. |

---

## 3. Dinamik / Token Korumalı Akışlar

* **Show Türk:** `ciner-live.ercdn.net` üzerinden yayın yapıyor ancak URL parametrelerinde kısa ömürlü `st=` ve `e=` tokenları zorunlu tutuluyor. Statik M3U'ya eklendiğinde birkaç saat içinde durmaktadır. Dinamik token üretici veya bypass yöntemi gerektirir.
* **NOW TV:** `nowtv.daioncdn.net` adresi kısa ömürlü st/e parametresi kullanıyor. CloudStream sağlayıcısında web sayfasından otomatik taze token çekilerek çözülmüştür; M3U tarafında ise TurkNet ercdn yansısı kullanılmaktadır.
* **Teve2:** Yayıncının origin ve referer doğrulaması vardır. CloudStream sağlayıcısında özel başlıklarla çözülmektedir.

---

## 4. Geliştirici ve Katkı Notu

Yeni bir çalışan yayın adresi bulduğunuzda veya yukarıdaki kanallardan biri için kalıcı HLS akışı keşfettiğinizde:
1. `scripts/validate-cloudstream.ps1` veya `test_channels.py` ile master ve `.ts` parçalarının 200 döndüğünü doğrulayın.
2. `turkiye_ulusal_tv.m3u` ve `TurkiyeTVProvider.kt` dosyalarına ekleyin.
3. Bu takip listesinden ilgili kaydı güncelleyin veya kaldırın.

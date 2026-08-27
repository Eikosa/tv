package com.eikosa.turkiyetv

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newLiveSearchResponse
import com.lagradost.cloudstream3.newLiveStreamLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

private data class TvChannel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String,
    val group: String,
    val channelNumber: Int,
)

private const val TEVE2_STREAM_URL = "https://demiroren.daioncdn.net/teve2/teve2_360p.m3u8"
private const val TEVE2_REFERER = "https://www.tv2.com.tr/"
private const val TEVE2_ORIGIN = "https://www.tv2.com.tr"
private const val TEVE2_APP_ID = "6aab838a-437e-4a1b-bbd0-e30f79cdbbbd"
private const val TEVE2_SID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
private const val TEVE2_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
private const val YOUTUBE_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
private const val YOUTUBE_ANDROID_USER_AGENT =
    "com.google.android.youtube/20.10.38 (Linux; U; Android 14)"
private const val FALLBACK_LOGO =
    "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/ulusal-tv-tr.png"

private fun tvGardenChannel(
    id: String,
    name: String,
    streamUrl: String,
    group: String,
    channelNumber: Int,
    logoUrl: String = FALLBACK_LOGO,
) = TvChannel(id, name, streamUrl, logoUrl, group, channelNumber)

private fun youtubeChannel(
    id: String,
    name: String,
    videoId: String,
    group: String,
    channelNumber: Int,
) = TvChannel(
    id = "YouTube_$id",
    name = name,
    streamUrl = "https://www.youtube.com/watch?v=$videoId",
    logoUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
    group = group,
    channelNumber = channelNumber,
)

class TurkiyeTVProvider : MainAPI() {
    override var mainUrl = "https://github.com/Eikosa/tv"
    override var name = "Türkiye Türkçe Canlı TV"
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.Live)
    override val hasMainPage = true
    override val mainPage = listOf(
        com.lagradost.cloudstream3.mainPage("general", "Genel / Ulusal"),
        com.lagradost.cloudstream3.mainPage("news", "Haber"),
        com.lagradost.cloudstream3.mainPage("sports", "Spor"),
        com.lagradost.cloudstream3.mainPage("music", "Müzik"),
        com.lagradost.cloudstream3.mainPage("kids", "Çocuk / Eğitim"),
        com.lagradost.cloudstream3.mainPage("culture", "Belgesel / Kültür"),
        com.lagradost.cloudstream3.mainPage("religion", "Dini"),
        com.lagradost.cloudstream3.mainPage("entertainment", "Eğlence / Yaşam"),
        com.lagradost.cloudstream3.mainPage("youtube", "YouTube / Dizi"),
        com.lagradost.cloudstream3.mainPage("local", "Yerel / Diğer"),
    )

    // Popüler ulusal ve haber kanalları önce, daha nadir/yerel kanallar sonra listelenir.
    // TV Garden'dan alınan yeni adresler de eklenmeden önce HTTP ve HLS playlist testiyle doğrulandı.
    private val tvGardenChannels = listOf(
        tvGardenChannel("TVGarden_ArasTV", "Aras TV", "https://2.rtmp.org/tv217/yayin.stream/playlist.m3u8", "Yerel", 101),
        tvGardenChannel("TVGarden_ASTV", "AS TV", "https://live.artidijitalmedya.com/artidijital_astv/astv/playlist.m3u8", "Yerel" , 102),
        tvGardenChannel("TVGarden_BiKanal", "Bi Kanal", "https://bikanal-live.ercdn.net/bikanal/bikanal.m3u8", "Yerel", 103),
        tvGardenChannel("TVGarden_BodrumKentTV", "Bodrum Kent TV", "https://edge2.taksimbilisim.com/bodrumkenttv/bant1/playlist.m3u8", "Yerel", 104),
        tvGardenChannel("TVGarden_BRTV", "BRTV", "https://live.artidijitalmedya.com/artidijital_brtv/brtv/playlist.m3u8", "Yerel", 106, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/brtv-tr.png"),
        tvGardenChannel("TVGarden_Cine1", "Cine 1", "https://canliyayin.cine1.com.tr/memfs/cbaef080-a742-4644-9e9e-2b9f6a5103c3_output_0.m3u8", "İş / Dizi", 107),
        tvGardenChannel("TVGarden_DenizPostasi", "Deniz Postası TV", "https://live.artidijitalmedya.com/artidijital_denizpostasi/denizpostasi/playlist.m3u8", "Haber", 108),
        tvGardenChannel("TVGarden_EkolSports", "Ekol Sports", "https://ekoltv-live.ercdn.net/ekolsport/ekolsport.m3u8", "Spor", 109),
        tvGardenChannel("TVGarden_EkolTV", "Ekol TV", "https://ekoltv-live.ercdn.net/ekoltv/ekoltv.m3u8", "Haber", 110),
        tvGardenChannel("TVGarden_ErciyesTV", "Erciyes TV", "https://live.artidijitalmedya.com/artidijital_erciyestv/erciyestv/playlist.m3u8", "Yerel", 111),
        tvGardenChannel("TVGarden_ERTV", "ERTV", "https://live.artidijitalmedya.com/artidijital_ertv_new/ertv/playlist.m3u8", "Yerel", 112),
        tvGardenChannel("TVGarden_Haber61", "Haber61 TV", "https://cdn-haber61tv.yayin.com.tr/haber61tv/smil:haber61tv.smil/index.m3u8", "Haber", 113),
        tvGardenChannel("TVGarden_HunatTV", "Hunat TV", "https://live.artidijitalmedya.com/artidijital_hunattv/hunattv/playlist.m3u8", "Yerel", 114),
        tvGardenChannel("TVGarden_IcelTV", "İçel TV", "https://edge1.taksimbilisim.com/iceltv/bant1/playlist.m3u8", "Yerel", 115),
        tvGardenChannel("TVGarden_Kanal15", "Kanal 15", "https://live.artidijitalmedya.com/artidijital_kanal15/kanal15/playlist.m3u8", "Yerel", 116, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal-15-tr.png"),
        tvGardenChannel("TVGarden_Kanal26", "Kanal 26", "https://live.artidijitalmedya.com/artidijital_kanal26/kanal26/playlist.m3u8", "Yerel", 117, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal-26-tr.png"),
        tvGardenChannel("TVGarden_Kanal3", "Kanal 3", "https://live.artidijitalmedya.com/artidijital_kanal3/kanal3/playlist.m3u8", "Yerel", 118, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal3-tr.png"),
        tvGardenChannel("TVGarden_Kanal33", "Kanal 33", "https://edge2.taksimbilisim.com/kanal33/bant1/playlist.m3u8", "Yerel", 119, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal-33-tr.png"),
        tvGardenChannel("TVGarden_Kanal34", "Kanal 34", "https://live.euromediacenter.com/kanal34/tracks-v1a1/playlist.m3u8", "Yerel", 120),
        tvGardenChannel("TVGarden_Kanal58", "Kanal 58", "https://live.artidijitalmedya.com/artidijital_kanal58/kanal58/playlist.m3u8", "Yerel", 121, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal-58-tr.png"),
        tvGardenChannel("TVGarden_Kanal7Avrupa", "Kanal 7 Avrupa", "https://livetv.radyotvonline.net/kanal7live/kanal7avr/playlist.m3u8", "Genel", 122, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal-7-avrupa-tr.png"),
        tvGardenChannel("TVGarden_KanalAvrupa", "Kanal Avrupa", "https://cdn-kanalavrupa.yayin.com.tr/kanalavrupa/tracks-v2a1/playlist.m3u8", "Genel", 123, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal-avrupa-tr.png"),
        tvGardenChannel("TVGarden_KanalB", "Kanal B", "https://tv.kanalb.tr/hls/kanalb/index.m3u8", "Haber", 124, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal-b-tr.png"),
        tvGardenChannel("TVGarden_KanalFirat", "Kanal Fırat", "https://live.artidijitalmedya.com/artidijital_kanalfirat/kanalfirat/playlist.m3u8", "Yerel", 125, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal-firat-tr.png"),
        tvGardenChannel("TVGarden_KanalHayat", "Kanal Hayat", "https://tbn02a.ltnschedule.com/hls/nx21i.m3u8", "Dini", 126),
        tvGardenChannel("TVGarden_KanalPlus", "Kanal Plus", "https://live.artidijitalmedya.com/artidijital_kanalplus/kanalplus/mpeg/playlist.m3u8", "Yerel", 127),
        tvGardenChannel("TVGarden_KanalV", "Kanal V", "https://live.artidijitalmedya.com/artidijital_kanalv/kanalv/playlist.m3u8", "Yerel", 128, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kanal-v-tr.png"),
        tvGardenChannel("TVGarden_KaradenizTV", "Karadeniz TV", "https://panel.5gtvhosting.com/hls/karadeniztv/karadeniztv.m3u8", "Yerel", 129),
        tvGardenChannel("TVGarden_KayTV", "Kay TV", "https://live.artidijitalmedya.com/artidijital_kaytv/kaytv/playlist.m3u8", "Yerel", 130),
        tvGardenChannel("TVGarden_KentTurk", "Kent Türk", "https://live.artidijitalmedya.com/artidijital_kentturktv/kentturktv/playlist.m3u8", "Yerel", 131, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/kent-turk-tr.png"),
        tvGardenChannel("TVGarden_LineTV", "Line TV", "https://edge2.taksimbilisim.com/linetv/bant1/playlist.m3u8", "Yerel", 132, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/line-tv-tr.png"),
        tvGardenChannel("TVGarden_LuysTV", "Luys TV", "https://b01c02nl.mediatriple.net/videoonlylive/mtpayqrfkgirxelive/broadcast_5e91c5ac96898.smil/playlist.m3u8", "Yerel", 133),
        tvGardenChannel("TVGarden_MaviKaradeniz", "MaviKaradeniz TV", "https://live.artidijitalmedya.com/artidijital_mavikaradeniz/mavikaradeniz/playlist.m3u8", "Yerel", 134, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/mavi-karadeniz-tr.png"),
        tvGardenChannel("TVGarden_MercanTV", "Mercan TV", "https://live.artidijitalmedya.com/artidijital_mercantv/mercantv/playlist.m3u8", "Yerel", 135, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/mercan-tv-tr.png"),
        tvGardenChannel("TVGarden_OlayTurk", "OlayTürk TV", "https://live.artidijitalmedya.com/artidijital_olayturk/olayturk/playlist.m3u8", "Haber", 136),
        tvGardenChannel("TVGarden_PowerAkustik", "PowerTürk Akustik", "https://livetv.powerapp.com.tr/pturkakustik/akustik.smil/playlist.m3u8", "Müzik", 137, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/powerturk-tr.png"),
        tvGardenChannel("TVGarden_PowerSlow", "PowerTürk Slow", "https://livetv.powerapp.com.tr/pturkslow/slow.smil/playlist.m3u8", "Müzik", 138, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/powerturk-tr.png"),
        tvGardenChannel("TVGarden_PowerTaptaze", "PowerTürk Taptaze", "https://livetv.powerapp.com.tr/pturktaptaze/taptaze.smil/playlist.m3u8", "Müzik", 139, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/powerturk-tr.png"),
        tvGardenChannel("TVGarden_RuhaTV", "Ruha TV", "https://ruhatv.radyotelekom.com.tr:3515/live/ruhatvlive.m3u8", "Yerel", 140),
        tvGardenChannel("TVGarden_Semerkand", "Semerkand TV", "https://b01c02nl.mediatriple.net/videoonlylive/mtisvwurbfcyslive/broadcast_58d915bd40efc.smil/playlist.m3u8", "Dini", 141, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/semerkand-tv-tr.png"),
        tvGardenChannel("TVGarden_SonmezTV", "Sönmez TV", "https://sonmeztv.ozelip.com.tr:3826/live/sonmeztvlive.m3u8", "Yerel", 142),
        tvGardenChannel("TVGarden_SunRTV", "Sun RTV", "https://live.artidijitalmedya.com/artidijital_sunrtv/sunrtv/playlist.m3u8", "Yerel", 143),
        tvGardenChannel("TVGarden_Telenews", "Telenews", "https://cdn-telenews.yayin.com.tr/telenews/tracks-v1a1/playlist.m3u8", "Haber", 144),
        tvGardenChannel("TVGarden_TonTV", "Ton TV", "https://live.artidijitalmedya.com/artidijital_tontv/tontv/playlist.m3u8", "Yerel", 145),
        tvGardenChannel("TVGarden_TrakyaTurk", "Trakya Türk", "https://live.euromediacenter.com/trakyaturk/tracks-v1a1/playlist.m3u8", "Yerel", 146),
        tvGardenChannel("TVGarden_TRTArabi", "TRT Arabi", "https://tv-trtarabi.medya.trt.com.tr/master.m3u8", "Genel", 147, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/trt-arabi-tr.png"),
        tvGardenChannel("TVGarden_TurkHaber", "TürkHaber TV", "https://edge2.taksimbilisim.com/turkhaber/bant1/playlist.m3u8", "Haber", 148, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/turk-haber-tr.png"),
        tvGardenChannel("TVGarden_TV41", "TV 41", "https://live.artidijitalmedya.com/artidijital_tv41/tv41/playlist.m3u8", "Yerel", 149),
        tvGardenChannel("TVGarden_TV52", "TV 52", "https://edge2.taksimbilisim.com/tv52/bant1/chunks.m3u8", "Yerel", 150, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/tv52-tr.png"),
        tvGardenChannel("TVGarden_TVDen", "TV Den", "https://canli.tvden.com.tr/hls/live.m3u8", "Yerel", 151, "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/tv-den-tr.png"),
        tvGardenChannel("TVGarden_TYTTurk", "TYT Türk", "https://tytturk-live.ercdn.net/tytturk/tytturk.m3u8", "Genel", 152),
    )

    // TV Garden ve kullanıcı tarafından verilen YouTube yayınları.
    // Canlı oynatma önce Android istemcili HLS çözümüyle, sonra yerleşik
    // CloudStream YoutubeExtractor'ıyla denenir.
    private val youtubeChannels = listOf(
        youtubeChannel("CNNTurk", "CNN TÜRK", "6N8_r2uwLEc", "Haber", 201),
        youtubeChannel("SozcuTV", "SÖZCÜ TV", "ztmY_cCtUl0", "Haber", 202),
        youtubeChannel("BloombergHT", "Bloomberg HT", "5ngQ40FQHv0", "Haber", 203),
        youtubeChannel("APara", "A Para", "Fas1VhgP8Uk", "Haber", 204),
        youtubeChannel("Benguturk", "Bengütürk TV", "MOhcWsOL1Us", "Haber", 205),
        youtubeChannel("KRT", "KRT TV", "_k0wG2Qah1g", "Haber", 206),
        youtubeChannel("TV100", "TV 100", "4WSvLRk83-c", "Haber", 208),
        youtubeChannel("UlusalKanal", "Ulusal Kanal", "Gcxkjxhbhk8", "Haber", 209),
        youtubeChannel("BeINSports", "beIN SPORTS Türkiye", "i7UpPgxfZZ8", "Spor", 210),
        youtubeChannel("CartoonNetwork", "Cartoon Network Türkiye", "JyMeD9wfMZQ", "Çocuk", 211),
        youtubeChannel("KralSakir", "Cartoon Network Türkiye: Kral Şakir", "5Whk9MVTpI4", "Çocuk", 212),
        youtubeChannel("Cartoonito", "Cartoonito Türkiye", "XDSd6m4SauI", "Çocuk", 213),
        youtubeChannel("Gumball", "Gumball", "9LDR3lHACKM", "Çocuk", 214),
        youtubeChannel("AskNeva", "Aşk-ı Nevâ", "2aAuvDd2tSo", "Dini", 215),
        youtubeChannel("CAHMedya", "CAH Medya", "DL99RVqsAvg", "Dini", 216),
        youtubeChannel("Ibrahimlive", "İbrahimlive", "uUL6u3mNQyg", "Dini", 217),
        youtubeChannel("AnkaraBB", "Ankara Büyükşehir Belediyesi", "B-84y-luaTs", "Yerel", 218),
        youtubeChannel("GuldurGuldur", "Güldür Güldür Show", "6iTXnuBXXqI", "Eğlence", 219),
        youtubeChannel("GulsahFilm", "Gülşah Film: Kemal Sunal Filmleri", "8AkJ9BgYB-c", "Dizi / YouTube", 220),
        youtubeChannel("KemalSunal", "Kemal Sunal: Filmleri ve Sahneleri", "qWDd505ciJQ", "Dizi / YouTube", 221),
        youtubeChannel("MuhteşemYuzyil", "Muhteşem Yüzyıl", "tZxD_s29JZc", "Dizi / YouTube", 222),
        youtubeChannel("LeylaMecnun", "Leyla ile Mecnun", "3nlND4audLg", "Dizi / YouTube", 223),
        youtubeChannel("Seksenler", "Seksenler", "qGYlF1MiMxw", "Dizi / YouTube", 224),
        youtubeChannel("AvrupaYakasi", "Avrupa Yakası", "NGsjOrzwjZk", "Dizi / YouTube", 225),
        youtubeChannel("AleminKirali", "Alemin Kıralı", "avDRwKKjeSI", "Dizi / YouTube", 226),
        youtubeChannel("YalanDunya", "Yalan Dünya", "Efh06uzzORM", "Dizi / YouTube", 227),
        youtubeChannel("AskMemnu", "Aşk-ı Memnu", "JpP13Wp1ke0", "Dizi / YouTube", 228),
        youtubeChannel("Yesilcam", "Yeşilçam", "NqbUZyS58u8", "Dizi / YouTube", 229),
        youtubeChannel("Adanali", "Adanalı", "sF1AgroEr60", "Dizi / YouTube", 230),
        youtubeChannel("EmretKomutanim", "Emret Komutanım", "b4U9nt3s128", "Dizi / YouTube", 231),
        youtubeChannel("CocuklarDuymasin", "Çocuklar Duymasın", "g7oVIIevRMk", "Dizi / YouTube", 232),
    )

    // 100 öncelikli ulusal/yerel yayın ve yukarıdaki doğrulanmış TV Garden yayınları.
    private val channels = listOf(
        TvChannel(
            id = "TRT1.tr@SD",
            name = "TRT 1",
            streamUrl = "https://tv-trt1.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/trt-1-tr.png",
            group = "Genel",
            channelNumber = 1,
        ),
        TvChannel(
            id = "ATV.tr@SD",
            name = "ATV",
            streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/atv/atv_1080p.m3u8",
            logoUrl = "https://i.imgur.com/HyVUwFC.png",
            group = "Genel",
            channelNumber = 2,
        ),
        TvChannel(
            id = "KanalD.tr@SD",
            name = "Kanal D",
            streamUrl = "https://demiroren.daioncdn.net/kanald/kanald.m3u8?app=kanald_web&ce=3",
            logoUrl = "https://i.imgur.com/9o1atM6.png",
            group = "Genel",
            channelNumber = 3,
        ),
        TvChannel(
            id = "Genel_ShowTV",
            name = "Show TV",
            streamUrl = "https://ciner.daioncdn.net/showtv/showtv.m3u8?app=showtv_web",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/show-tr.png",
            group = "Genel",
            channelNumber = 4,
        ),
        TvChannel(
            id = "StarTV.tr@SD",
            name = "Star TV",
            streamUrl = "https://dogus.daioncdn.net/startv/startv_720p.m3u8?app=a20ac41e-bdc3-4aa1-934d-26b484480ac9&ce=3&sid=8l4w3lst4co5",
            logoUrl = "https://i.imgur.com/9O3DHRB.png",
            group = "Genel",
            channelNumber = 5,
        ),
        TvChannel(
            id = "NOWTV.tr@SD",
            name = "NOW TV",
            streamUrl = "https://uycyyuuzyh.turknet.ercdn.net/nphindgytw/nowtv/nowtv.m3u8",
            logoUrl = "https://i.imgur.com/5EYjWK7.png",
            group = "Eğlence",
            channelNumber = 6,
        ),
        TvChannel(
            id = "TV8.tr@SD",
            name = "TV 8",
            streamUrl = "https://tv8.daioncdn.net/tv8/tv8.m3u8?app=7ddc255a-ef47-4e81-ab14-c0e5f2949788&ce=3",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/tv8-tr.png",
            group = "Genel",
            channelNumber = 7,
        ),
        TvChannel(
            id = "TV360.tr@SD",
            name = "360",
            streamUrl = "https://turkmedya-live.ercdn.net/tv360/tv360.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/360-tr.png",
            group = "Genel",
            channelNumber = 8,
        ),
        TvChannel(
            id = "Teve2.tr@SD",
            name = "Teve2",
            streamUrl = TEVE2_STREAM_URL,
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/teve2-tr.png",
            group = "Genel",
            channelNumber = 99,
        ),
        TvChannel(
            id = "TRTHaber.tr@SD",
            name = "TRT Haber",
            streamUrl = "https://tv-trthaber.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/trt-haber-tr.png",
            group = "Haber",
            channelNumber = 8,
        ),
        TvChannel(
            id = "NTV.tr@SD",
            name = "NTV",
            streamUrl = "https://dogus.daioncdn.net/ntv/ntv.m3u8?app=ntv_web",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/ntv-tr.png",
            group = "Genel",
            channelNumber = 9,
        ),
        TvChannel(
            id = "Haber_HaberTurk",
            name = "Haber Türk",
            streamUrl = "https://rmtftbjlne.turknet.ercdn.net/bpeytmnqyp/haberturktv/haberturktv_1080p.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/haberturk-tr.png",
            group = "Haber",
            channelNumber = 10,
        ),
        TvChannel(
            id = "Haber_HaberGlobal",
            name = "Haber Global",
            streamUrl = "https://tv.ensonhaber.com/haberglobal/haberglobal.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/haber-global-tr.png",
            group = "Haber",
            channelNumber = 11,
        ),
        TvChannel(
            id = "TGRTHaber.tr@SD",
            name = "TGRT Haber",
            streamUrl = "https://canli.tgrthaber.com/tgrt.m3u8",
            logoUrl = "https://i.imgur.com/PrxwKDw.png",
            group = "Haber",
            channelNumber = 12,
        ),
        TvChannel(
            id = "AHaber.tr@SD",
            name = "A Haber",
            streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/a-haber-tr.png",
            group = "Haber",
            channelNumber = 13,
        ),
        TvChannel(
            id = "HalkTV.tr@SD",
            name = "Halk TV",
            streamUrl = "https://halktv-live.daioncdn.net/halktv/halktv.m3u8",
            logoUrl = "https://i.imgur.com/xM0HA30.png",
            group = "Haber",
            channelNumber = 14,
        ),
        TvChannel(
            id = "24TV.tr@SD",
            name = "TV 24",
            streamUrl = "https://turkmedya-live.ercdn.net/tv24/tv24.m3u8",
            logoUrl = "https://i.imgur.com/8FO41es.png",
            group = "Haber",
            channelNumber = 15,
        ),
        TvChannel(
            id = "TVNET.tr@SD",
            name = "TVNET",
            streamUrl = "https://tvnet-live.lg.mncdn.com/tvnet/tvnet/playlist.m3u8",
            logoUrl = "https://i.imgur.com/mQo8yWQ.png",
            group = "Genel",
            channelNumber = 16,
        ),
        TvChannel(
            id = "ASpor.tr@SD",
            name = "A Spor",
            streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/aspor/aspor.m3u8",
            logoUrl = "https://i.imgur.com/ZhkZzLf.png",
            group = "Spor",
            channelNumber = 17,
        ),
        TvChannel(
            id = "Spor_TRT",
            name = "TRT Spor",
            streamUrl = "https://trt.daioncdn.net/trtspor/master.m3u8?app=web",
            logoUrl = "https://cdn-i.pr.trt.com.tr/trttv/w750/h750/q100/13687983.png",
            group = "Spor",
            channelNumber = 18,
        ),
        TvChannel(
            id = "Spor_SporYildiz",
            name = "TRT Spor Yıldız",
            streamUrl = "https://trt.daioncdn.net/trtspor-yildiz/master.m3u8?app=web&platform=trtspor",
            logoUrl = "https://cdn-i.pr.trt.com.tr/trttv/w750/h750/q100/11290418.png",
            group = "Spor",
            channelNumber = 19,
        ),
        TvChannel(
            id = "TRT_2",
            name = "TRT 2",
            streamUrl = "https://tv-trt2.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://cdn-i.pr.trt.com.tr/trttv/w750/h750/q100/7027954.png",
            group = "Genel",
            channelNumber = 20,
        ),
        TvChannel(
            id = "Genel_Kanal7",
            name = "Kanal 7",
            streamUrl = "https://kanal7-live.daioncdn.net/kanal7/kanal7.m3u8",
            logoUrl = "https://pbs.twimg.com/profile_images/1237020778450817025/A5oEtr15_400x400.png",
            group = "Genel",
            channelNumber = 21,
        ),
        TvChannel(
            id = "Genel_BeyazTV",
            name = "Beyaz TV",
            streamUrl = "https://beyaztv-live.daioncdn.net/beyaztv/beyaztv_720p.m3u8",
            logoUrl = "https://www.beyaztv.com.tr/images/logo.png",
            group = "Genel",
            channelNumber = 22,
        ),
        TvChannel(
            id = "TV4.tr@SD",
            name = "TV4",
            streamUrl = "https://turkmedya-live.ercdn.net/tv4/tv4.m3u8",
            logoUrl = "https://i.imgur.com/UpsQsbd.png",
            group = "Genel",
            channelNumber = 23,
        ),
        TvChannel(
            id = "TRT3.tr@SD",
            name = "TRT 3",
            streamUrl = "https://tv-trt3.live.trt.com.tr/master.m3u8",
            logoUrl = "https://i.imgur.com/JrWFwBd.png",
            group = "Genel",
            channelNumber = 24,
        ),
        TvChannel(
            id = "TRTBelgesel.tr@SD",
            name = "TRT Belgesel",
            streamUrl = "https://tv-trtbelgesel.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/trt-belgesel-tr.png",
            group = "Belgesel",
            channelNumber = 25,
        ),
        TvChannel(
            id = "TRTCocuk.tr@SD",
            name = "TRT Cocuk",
            streamUrl = "https://tv-trtcocuk.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/trt-cocuk-tr.png",
            group = "Çocuk",
            channelNumber = 26,
        ),
        TvChannel(
            id = "MinikaGo.tr@SD",
            name = "Minika Go",
            streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago/minikago.m3u8",
            logoUrl = "https://i.imgur.com/qIoipDq.png",
            group = "Çocuk",
            channelNumber = 27,
        ),
        TvChannel(
            id = "MinikaCocuk.tr@SD",
            name = "Minika Cocuk",
            streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/minikago_cocuk/minikago_cocuk.m3u8",
            logoUrl = "https://i.imgur.com/VCywMTv.png",
            group = "Çocuk",
            channelNumber = 28,
        ),
        TvChannel(
            id = "DreamTurk.tr@SD",
            name = "Dream Türk",
            streamUrl = "https://live.duhnet.tv/S2/HLS_LIVE/dreamturknp/playlist.m3u8",
            logoUrl = "https://i.imgur.com/vJ8VaZi.png",
            group = "Müzik",
            channelNumber = 29,
        ),
        TvChannel(
            id = "Muzik_PowerTurk",
            name = "Power Türk",
            streamUrl = "https://livetv.powerapp.com.tr/powerturkTV/powerturkhd.smil/playlist.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/powerturk-tr.png",
            group = "Müzik",
            channelNumber = 30,
        ),
        TvChannel(
            id = "KralPopTV.tr@SD",
            name = "KRAL Pop TV",
            streamUrl = "https://dogus-live.daioncdn.net/kralpoptv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/ch365lh.png",
            group = "Müzik",
            channelNumber = 31,
        ),
        TvChannel(
            id = "Muzik_NumberOneTurk",
            name = "NumberOne Türk",
            streamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e187770143.smil/playlist.m3u8",
            logoUrl = "https://raw.githubusercontent.com/Eikosa/tv/main/logos/numberone-turk.svg",
            group = "Müzik",
            channelNumber = 32,
        ),
        TvChannel(
            id = "4UTV.tr@SD",
            name = "4U TV",
            streamUrl = "https://hls.4utv.live/hls/stream.m3u8",
            logoUrl = "https://i.imgur.com/PexhKwp.png",
            group = "Eğlence",
            channelNumber = 33,
        ),
        TvChannel(
            id = "A2TV.tr@SD",
            name = "A2TV",
            streamUrl = "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/a2tv/a2tv.m3u8",
            logoUrl = "https://iatv.tmgrup.com.tr/site/v2/a2tv/i/a2tv-logo.png",
            group = "Genel",
            channelNumber = 34,
        ),
        TvChannel(
            id = "AksuTV.tr@SD",
            name = "Aksu TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_aksutv/aksutv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/mgDCKiy.png",
            group = "Genel",
            channelNumber = 35,
        ),
        TvChannel(
            id = "AlanyaPostaTV.tr@SD",
            name = "Alanya Posta TV",
            streamUrl = "https://api-tv3.yayin.com.tr/postatv/postatv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/a3w9eFe.png",
            group = "Genel",
            channelNumber = 36,
        ),
        TvChannel(
            id = "AltasTV.tr@SD",
            name = "Altas TV",
            streamUrl = "https://edge1.socialsmart.tv/altastv/bant1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/XaJ7fp6.png",
            group = "Genel",
            channelNumber = 37,
        ),
        TvChannel(
            id = "AnadoluNetTV.tr@SD",
            name = "Anadolu Net TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_anadolunet/anadolunet/playlist.m3u8",
            logoUrl = "https://www.anadolunettv.com/d/r/logo.png",
            group = "Genel",
            channelNumber = 38,
        ),
        TvChannel(
            id = "ATVAlanya.tr@SD",
            name = "ATV Alanya",
            streamUrl = "https://cdn-alanyatv.yayin.com.tr/alanyatv/alanyatv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/bUwarR0.png",
            group = "Genel",
            channelNumber = 39,
        ),
        TvChannel(
            id = "BirTV.tr@SD",
            name = "Bir TV",
            streamUrl = "https://edge.taksimbilisim.com/birtv/bant1/playlist.m3u8",
            logoUrl = "https://birtv.tv/assets/upload/637f83951df60.png",
            group = "Genel",
            channelNumber = 40,
        ),
        TvChannel(
            id = "ASTV.tr@SD",
            name = "Bursa AS TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_astv/astv/playlist.m3u8",
            logoUrl = "https://asset.artidijitalmedya.com/image/188x188/channels/v1/logo_13.png",
            group = "Haber",
            channelNumber = 41,
        ),
        TvChannel(
            id = "CayTV.tr@SD",
            name = "Cay TV",
            streamUrl = "https://edge1.socialsmart.tv/caytv/bant1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/ndupxwu.png",
            group = "Genel",
            channelNumber = 42,
        ),
        TvChannel(
            id = "CNBCe.tr@SD",
            name = "CNBC-e",
            streamUrl = "https://hnpsechtsc.turknet.ercdn.net/xpnvudnlsv/cnbc-e/cnbc-e.m3u8",
            logoUrl = "https://s.cnbce.com/dist/images/logo-nav.png",
            group = "İş / Dizi",
            channelNumber = 43,
        ),
        TvChannel(
            id = "Belgesel_CiftciTV",
            name = "Çiftçi TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_ciftcitv/ciftcitv/playlist.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/ciftci-tv-tr.png",
            group = "Belgesel",
            channelNumber = 44,
        ),
        TvChannel(
            id = "DiyanetTV.tr@SD",
            name = "Diyanet TV",
            streamUrl = "https://eustr73.mediatriple.net/videoonlylive/mtikoimxnztxlive/broadcast_5e3bf95a47e07.smil/playlist.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/diyanet-tv-tr.png",
            group = "Dini",
            channelNumber = 45,
        ),
        TvChannel(
            id = "DiyarTV.tr@SD",
            name = "Diyar TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_diyartv/diyartv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/qWkHjRA.png",
            group = "Dini",
            channelNumber = 46,
        ),
        TvChannel(
            id = "DostTV.tr@SD",
            name = "Dost TV",
            streamUrl = "https://dost.stream.emsal.im/tv/live.m3u8",
            logoUrl = "https://dosttv.com/wp-content/uploads/2022/02/dost_logo.png",
            group = "Dini",
            channelNumber = 47,
        ),
        TvChannel(
            id = "EdessaTV.tr@SD",
            name = "Edessa TV",
            streamUrl = "https://canli.edessatv.com/hls/stream.m3u8",
            logoUrl = "https://i.imgur.com/rhU6j9I.png",
            group = "Eğlence",
            channelNumber = 48,
        ),
        TvChannel(
            id = "ErzurumWebTV.tr@SD",
            name = "Erzurum Web TV",
            streamUrl = "https://win29.yayin.com.tr/erzurumwebtv/erzurumwebtv/iptvdelisi.m3u8",
            logoUrl = "https://i.imgur.com/HF7N4Li.png",
            group = "Genel",
            channelNumber = 49,
        ),
        TvChannel(
            id = "ESTV.tr@SD",
            name = "ES TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_estv/estv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/epAEBrp.png",
            group = "Genel",
            channelNumber = 50,
        ),
        TvChannel(
            id = "ETVKayseri.tr@SD",
            name = "ETV Kayseri",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_etv/etv/playlist.m3u8",
            logoUrl = "https://yt3.googleusercontent.com/ytc/AIdro_msPtG6zfPKIJIIdvG4dehXLoVoXcHM2-2HL9YxsNYzGQ=s512-c-k-c0x00ffffff-no-rj",
            group = "Genel",
            channelNumber = 51,
        ),
        TvChannel(
            id = "EuroD.tr@SD",
            name = "Euro D",
            streamUrl = "https://live.duhnet.tv/S2/HLS_LIVE/eurodnp/playlist.m3u8",
            logoUrl = "https://i.imgur.com/x9kHsXo.png",
            group = "Genel",
            channelNumber = 52,
        ),
        TvChannel(
            id = "FinansTurkTV.tr@SD",
            name = "Finans Turk TV",
            streamUrl = "https://yayin30.haber100.com/live/finansturk/playlist.m3u8",
            logoUrl = "https://i.ibb.co/wBwmB1T/iY0osc7.png",
            group = "Haber",
            channelNumber = 53,
        ),
        TvChannel(
            id = "FortunaTV.tr@SD",
            name = "Fortuna TV",
            streamUrl = "https://edge1.socialsmart.tv/ftvturk/bant1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/ZbUSlOC.png",
            group = "Yaşam",
            channelNumber = 54,
        ),
        TvChannel(
            id = "GuneydoguTV.tr@SD",
            name = "Guneydogu TV",
            streamUrl = "https://edge1.socialsmart.tv/gtv/bant1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/JT5pp3Y.png",
            group = "Haber",
            channelNumber = 55,
        ),
        TvChannel(
            id = "GZT.tr@SD",
            name = "GZT",
            streamUrl = "https://gzttv-live.lg.mncdn.com/gzttv/gzttv/playlist.m3u8",
            logoUrl = FALLBACK_LOGO,
            group = "Belgesel / Dizi",
            channelNumber = 56,
        ),
        TvChannel(
            id = "HTSporTV.tr@SD",
            name = "HTSpor TV",
            streamUrl = "https://ciner.daioncdn.net/ht-spor/ht-spor.m3u8?app=web",
            logoUrl = "https://www.htspor.com/images/manifest/social-share-logo.png",
            group = "Spor",
            channelNumber = 57,
        ),
        TvChannel(
            id = "IlkeTV.tr@HD",
            name = "Ilke TV",
            streamUrl = "https://stream.ilketv.com.tr/hls/ilkecanli.m3u8",
            logoUrl = "https://ilketv.com.tr/wp-content/uploads/2024/06/logo.png",
            group = "Genel",
            channelNumber = 58,
        ),
        TvChannel(
            id = "Kanal12.tr@SD",
            name = "Kanal 12",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_kanal12/kanal12/playlist.m3u8",
            logoUrl = "https://i.imgur.com/51xCkXG.png",
            group = "Genel",
            channelNumber = 59,
        ),
        TvChannel(
            id = "Kanal23.tr@SD",
            name = "Kanal 23",
            streamUrl = "https://cdn-kanal23.yayin.com.tr/kanal23/index.m3u8",
            logoUrl = "https://i.imgur.com/3br8RCq.png",
            group = "Genel",
            channelNumber = 60,
        ),
        TvChannel(
            id = "KNMusicTV.az@SD",
            name = "KN Music TV",
            streamUrl = "https://cdn4.yayin.com.tr/kntv/tracks-v1a1/mono.m3u8",
            logoUrl = "https://raw.githubusercontent.com/Eikosa/tv/main/logos/kn-music-tv.svg",
            group = "Müzik",
            channelNumber = 61,
        ),
        TvChannel(
            id = "KocaeliTV.tr@SD",
            name = "Kocaeli TV",
            streamUrl = "https://edge.taksimbilisim.com/kocaelitv/bant1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/LeTP4zq.png",
            group = "Genel",
            channelNumber = 62,
        ),
        TvChannel(
            id = "KonyaOlayTV.tr@SD",
            name = "Konya Olay TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_konyaolaytv/konyaolaytv/playlist.m3u8",
            logoUrl = "https://www.konyaolaytv.com/upload/tema/20230307__5154687762.jpg",
            group = "Genel",
            channelNumber = 63,
        ),
        TvChannel(
            id = "LalegulTV.tr@SD",
            name = "Lalegul TV",
            streamUrl = "https://lbl.netmedya.net/hls/lalegultv.m3u8",
            logoUrl = "https://i.imgur.com/wwCysFs.png",
            group = "Dini",
            channelNumber = 64,
        ),
        TvChannel(
            id = "Genel_LifeTV",
            name = "Life TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_lifetv/lifetv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/lWeJxm0.png",
            group = "Genel",
            channelNumber = 65,
        ),
        TvChannel(
            id = "MeltemTV.tr@SD",
            name = "Meltem TV",
            streamUrl = "https://vhxyrsly.rocketcdn.com/meltemtv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/C3m6w5S.png",
            group = "Genel",
            channelNumber = 66,
        ),
        TvChannel(
            id = "Number1Ask.tr@SD",
            name = "Number 1 Ask",
            streamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e18f9cea15_1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/slwbux7.png",
            group = "Müzik",
            channelNumber = 67,
        ),
        TvChannel(
            id = "Number1Damar.tr@SD",
            name = "Number 1 Damar",
            streamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e198784bdc_1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/rYtbAGZ.png",
            group = "Müzik",
            channelNumber = 68,
        ),
        TvChannel(
            id = "Number1Dance.tr@SD",
            name = "Number 1 Dance",
            streamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/u_stream_5c9e2aa8acf44_1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/ZM4PSyq.png",
            group = "Müzik",
            channelNumber = 69,
        ),
        TvChannel(
            id = "Number1TV.tr@SD",
            name = "Number 1 TV",
            streamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtkgeuihrlfwlive/broadcast_5c9e17cd59e8b.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/02cDIBi.png",
            group = "Müzik",
            channelNumber = 70,
        ),
        TvChannel(
            id = "PowerDance.tr@SD",
            name = "Power Dance",
            streamUrl = "https://livetv.powerapp.com.tr/dance/dance.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/QpPteBO.png",
            group = "Müzik",
            channelNumber = 71,
        ),
        TvChannel(
            id = "PowerLove.tr@SD",
            name = "Power Love",
            streamUrl = "https://livetv.powerapp.com.tr/plove/love.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/0RzUswR.png",
            group = "Müzik",
            channelNumber = 72,
        ),
        TvChannel(
            id = "PowerTurkAkustik.tr@SD",
            name = "Power Türk Akustik",
            streamUrl = "https://livetv.powerapp.com.tr/pturkakustik/akustik.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/driabBO.png",
            group = "Müzik",
            channelNumber = 73,
        ),
        TvChannel(
            id = "PowerTurkSlow.tr@SD",
            name = "Power Türk Slow",
            streamUrl = "https://livetv.powerapp.com.tr/pturkslow/slow.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/tQSoQXW.png",
            group = "Müzik",
            channelNumber = 74,
        ),
        TvChannel(
            id = "PowerTurkTaptaze.tr@SD",
            name = "Power Türk Taptaze",
            streamUrl = "https://livetv.powerapp.com.tr/pturktaptaze/taptaze.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/a5nW3HU.png",
            group = "Müzik",
            channelNumber = 75,
        ),
        TvChannel(
            id = "PowerTV.tr@SD",
            name = "Power TV",
            streamUrl = "https://livetv.powerapp.com.tr/powerTV/powerhd.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/XSL1gd7.png",
            group = "Genel",
            channelNumber = 76,
        ),
        TvChannel(
            id = "Sat7Turk.cy@SD",
            name = "Sat7 Türk",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_sat7turk/sat7turk/playlist.m3u8",
            logoUrl = "https://i.imgur.com/cKQIK4i.png",
            group = "Genel",
            channelNumber = 77,
        ),
        TvChannel(
            id = "SercemTV.tr@HD",
            name = "Sercem TV",
            streamUrl = "https://canli.sercemtv.com.tr/hls/0/stream.m3u8",
            logoUrl = "https://sercemtv.com.tr/sercemlogo.jpg",
            group = "Kültür / Genel",
            channelNumber = 78,
        ),
        TvChannel(
            id = "TBMMTV.tr@SD",
            name = "TBMM TV",
            streamUrl = "https://meclistv-live.ercdn.net/meclistv/meclistv.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/tbmm-tv-tr.png",
            group = "Genel",
            channelNumber = 79,
        ),
        TvChannel(
            id = "Tele1.tr@SD",
            name = "Tele 1",
            streamUrl = "https://tele1-live.ercdn.net/tele1/tele1.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/tele1-tr.png",
            group = "Genel",
            channelNumber = 80,
        ),
        TvChannel(
            id = "TempoTV.tr@SD",
            name = "Tempo TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_tempotv/tempotv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/tZpx32y.png",
            group = "Genel",
            channelNumber = 81,
        ),
        TvChannel(
            id = "Tivi6.tr@SD",
            name = "Tivi 6",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_tivi6/tivi6/playlist.m3u8",
            logoUrl = "https://i.imgur.com/Mbi0jbz.png",
            group = "Genel",
            channelNumber = 82,
        ),
        TvChannel(
            id = "TJKTV.tr@SD",
            name = "TJK TV",
            streamUrl = "https://tjktv-live.tjk.org/tjktv.m3u8",
            logoUrl = "https://i.imgur.com/3zHdkYG.png",
            group = "Spor",
            channelNumber = 83,
        ),
        TvChannel(
            id = "TRTAvaz.tr@SD",
            name = "TRT Avaz",
            streamUrl = "https://tv-trtavaz.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/trt-avaz-tr.png",
            group = "Genel",
            channelNumber = 84,
        ),
        TvChannel(
            id = "TRTDiyanetCocuk.tr@SD",
            name = "TRT Diyanet Çocuk",
            streamUrl = "https://tv-trtdiyanetcocuk.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://i.imgur.com/8PmXz9t.png",
            group = "Çocuk",
            channelNumber = 85,
        ),
        TvChannel(
            id = "TRTEBAIlkokul.tr@SD",
            name = "TRT EBA Ilkokul",
            streamUrl = "https://tv-e-okul00.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://i.imgur.com/CRBfZi4.png",
            group = "Eğitim",
            channelNumber = 86,
        ),
        TvChannel(
            id = "TRTEBALise.tr@SD",
            name = "TRT EBA Lise",
            streamUrl = "https://tv-e-okul02.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://i.imgur.com/vj2L2L2.png",
            group = "Eğitim",
            channelNumber = 87,
        ),
        TvChannel(
            id = "TRTEBA.tr@SD",
            name = "TRT EBA Ortaokul",
            streamUrl = "https://tv-e-okul01.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/eba-tv-ortaokul-tr.png",
            group = "Eğitim",
            channelNumber = 88,
        ),
        TvChannel(
            id = "TRTKurdi.tr@SD",
            name = "TRT Kurdî",
            streamUrl = "https://tv-trtkurdi.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/trt-kurdi-tr.png",
            group = "Genel",
            channelNumber = 89,
        ),
        TvChannel(
            id = "TRTMuzik.tr@SD",
            name = "TRT Müzik",
            streamUrl = "https://tv-trtmuzik.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://i.imgur.com/JgUzRH8.png",
            group = "Müzik",
            channelNumber = 90,
        ),
        TvChannel(
            id = "TRTTurk.tr@SD",
            name = "TRT Türk",
            streamUrl = "https://tv-trtturk.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/turkey/trt-turk-tr.png",
            group = "Genel",
            channelNumber = 91,
        ),
        TvChannel(
            id = "TurkHaberTV.tr@SD",
            name = "TürkHaber",
            streamUrl = "https://edge1.socialsmart.tv/turkhaber/bant1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/2AcRKdL.png",
            group = "Haber",
            channelNumber = 92,
        ),
        TvChannel(
            id = "TV1.tr@SD",
            name = "TV 1",
            streamUrl = "https://edge1.socialsmart.tv/tv1/bant1/playlist.m3u8",
            logoUrl = "https://i.imgur.com/8CtrYVb.png",
            group = "Genel",
            channelNumber = 93,
        ),
        TvChannel(
            id = "TV264.tr@SD",
            name = "TV 264",
            streamUrl = "https://b01c02nl.mediatriple.net/videoonlylive/mtdxkkitgbrckilive/broadcast_5ee244263fd6d.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/tudXdOZ.png",
            group = "Genel",
            channelNumber = 94,
        ),
        TvChannel(
            id = "UrfaNatikTV.tr@SD",
            name = "Urfa Natik TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_urfanatiktv/urfanatiktv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/8KO0uxn.png",
            group = "Genel",
            channelNumber = 95,
        ),
        TvChannel(
            id = "Van65TV.tr@SD",
            name = "Van 65 TV",
            streamUrl = "https://live.artidijitalmedya.com/artidijital_van65/van65/playlist.m3u8",
            logoUrl = "https://van65tv.com/assets/images/logo/logo.png",
            group = "Genel",
            channelNumber = 96,
        ),
        TvChannel(
            id = "VavTV.tr@SD",
            name = "Vav TV",
            streamUrl = "https://playlist.fasttvcdn.com/pl/rfrk9821hdy9dayo8wfyha/kltr-sanat-tv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/jw0gB8L.png",
            group = "Genel",
            channelNumber = 97,
        ),
        TvChannel(
            id = "YOLTV.de@SD",
            name = "YOL TV",
            streamUrl = "https://live.yoltv.com/hls/stream.m3u8",
            logoUrl = "https://i.imgur.com/rTBX6lS.png",
            group = "Genel",
            channelNumber = 98,
        ),
    ) + tvGardenChannels + youtubeChannels

    private val popularNewsOrder = listOf(
        "CNN TÜRK",
        "SÖZCÜ TV",
        "Haber Türk",
        "TRT Haber",
        "Haber Global",
        "Bloomberg HT",
        "TGRT Haber",
        "A Haber",
        "Halk TV",
        "TV 24",
        "KRT TV",
        "TV 100",
        "Ulusal Kanal",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val categorizedChannels = when (request.data) {
            // Her yayın tam olarak bir kategoriye düşer; ayrı bir "Öne Çıkanlar"
            // satırı olmadığı için aynı kanal ikinci kez gösterilmez.
            "general" -> channels.filter {
                it.group == "Genel" && (it.channelNumber <= 32 || it.id == "Teve2.tr@SD")
            }
            "news" -> channels.filter { it.group == "Haber" }.sortedBy {
                popularNewsOrder.indexOf(it.name).let { index ->
                    if (index == -1) Int.MAX_VALUE else index
                }
            }
            "sports" -> channels.filter { it.group == "Spor" }
            "music" -> channels.filter { it.group == "Müzik" }
            "kids" -> channels.filter { it.group == "Çocuk" || it.group == "Eğitim" }
            "culture" -> channels.filter {
                it.group.startsWith("Belgesel") || it.group.startsWith("Kültür")
            }
            "religion" -> channels.filter { it.group == "Dini" }
            "entertainment" -> channels.filter {
                it.group == "Eğlence" || it.group == "Yaşam" || it.group == "İş / Dizi"
            }
            "youtube" -> channels.filter { it.group == "Dizi / YouTube" }
            "local" -> channels.filter {
                (it.group == "Genel" && it.channelNumber > 32 && it.id != "Teve2.tr@SD") ||
                    it.group == "Yerel"
            }
            else -> emptyList()
        }
        val items = categorizedChannels.map { it.toSearchResponse() }
        return newHomePageResponse(request.name, items, false)
    }

    private val knownCategoryGroups = setOf(
        "Genel",
        "Haber",
        "Spor",
        "Müzik",
        "Çocuk",
        "Eğitim",
        "Belgesel",
        "Belgesel / Dizi",
        "Kültür / Genel",
        "Dini",
        "Eğlence",
        "Yaşam",
        "İş / Dizi",
        "Dizi / YouTube",
        "Yerel",
    )

    override suspend fun search(query: String): List<SearchResponse> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return channels.map { it.toSearchResponse() }

        return channels.filter { channel ->
            channel.name.lowercase().contains(normalizedQuery) ||
                channel.id.lowercase().contains(normalizedQuery) ||
                channel.group.lowercase().contains(normalizedQuery)
        }.map { it.toSearchResponse() }
    }

    override suspend fun load(url: String) =
        channels.firstOrNull { it.streamUrl == url || it.id == url }?.let { channel ->
            newLiveStreamLoadResponse(channel.name, channel.streamUrl, channel.streamUrl) {
                posterUrl = channel.logoUrl
                plot = "${channel.name}, Türkiye'de herkese açık canlı televizyon yayını."
                tags = listOf("Türkiye", channel.group, "Canlı TV")
            }
        } ?: error("Bilinmeyen kanal: $url")

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (com.lagradost.cloudstream3.SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        if (isYouTubeUrl(data)) {
            // Bazı CloudStream/NewPipe sürümleri canlı yayında loadExtractor'dan
            // true döndürüp hiç link üretmeyebiliyor. Önce doğrudan YouTube'un
            // canlı HLS manifestini almayı deniyoruz; olmazsa yerleşik çözücülere
            // geri dönüyoruz.
            if (resolveYouTubeLive(data, callback)) return true

            var extracted = false
            val trackingCallback: (ExtractorLink) -> Unit = { link ->
                extracted = true
                callback(link)
            }
            runCatching { loadExtractor(data, subtitleCallback, trackingCallback) }
            return extracted
        }

        if (data.substringBefore("?").substringBefore("#") == TEVE2_STREAM_URL) {
            return resolveTeve2Stream(callback)
        }

        val streamPath = data.substringBefore("?").substringBefore("#")
        if (!data.startsWith("https://") || !streamPath.endsWith(".m3u8")) return false

        callback(newExtractorLink(source = name, name = "${name} • HLS", url = data, type = ExtractorLinkType.M3U8) {
            referer = ""
            quality = Qualities.Unknown.value
        })
        return true
    }

    /**
     * YouTube canlı yayınları için CloudStream sürümünden bağımsız HLS çözümü.
     * Watch sayfasındaki güncel player yapılandırmasını kullanır; sabit ve
     * çabuk eskiyen bir API anahtarına güvenmez.
     */
    private suspend fun resolveYouTubeLive(
        data: String,
        callback: (ExtractorLink) -> Unit,
    ): Boolean = runCatching {
        val videoId = Regex(
            "(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:.*v=|v/|u/\\w/|embed/|shorts/|live/))([\\w-]{11})",
        ).find(data)?.groupValues?.get(1) ?: return@runCatching false

        val requestHeaders = mapOf(
            "User-Agent" to YOUTUBE_USER_AGENT,
            "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        )
        val page = app.get(
            "https://www.youtube.com/watch?v=$videoId",
            headers = requestHeaders,
            referer = "https://www.youtube.com/",
        )
        if (!page.isSuccessful) return@runCatching false

        val pageText = page.text
        val apiKey = Regex("\\\"INNERTUBE_API_KEY\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
            .find(pageText)?.groupValues?.get(1) ?: return@runCatching false

        val body = """
            {
              "context": {
                "client": {
                  "hl": "tr",
                  "gl": "TR",
                  "clientName": "ANDROID",
                  "clientVersion": "20.10.38"
                }
              },
              "videoId": "$videoId",
              "playbackContext": {
                "contentPlaybackContext": {
                  "html5Preference": "HTML5_PREF_WANTS"
                }
              }
            }
        """.trimIndent().toRequestBody("application/json; charset=utf-8".toMediaType())

        val player = app.post(
            "https://www.youtube.com/youtubei/v1/player?key=$apiKey",
            headers = mapOf(
                "User-Agent" to YOUTUBE_ANDROID_USER_AGENT,
                "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                "Content-Type" to "application/json",
            ),
            requestBody = body,
            referer = "https://www.youtube.com/",
        )
        if (!player.isSuccessful) return@runCatching false

        val hlsUrl = Regex("\\\"hlsManifestUrl\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
            .find(player.text)?.groupValues?.get(1)
            ?.replace("\\u0026", "&")
            ?.replace("\\/", "/")
            ?: return@runCatching false

        callback(
            newExtractorLink(
                source = name,
                name = "YouTube Live • HLS",
                url = hlsUrl,
                type = ExtractorLinkType.M3U8,
            ) {
                referer = "https://www.youtube.com/"
                quality = Qualities.Unknown.value
                headers = mapOf("User-Agent" to YOUTUBE_ANDROID_USER_AGENT)
            },
        )
        true
    }.getOrDefault(false)

    private fun generateTeve2Sid(): String = buildString(12) {
        repeat(12) {
            append(TEVE2_SID_ALPHABET.random())
        }
    }

    private suspend fun resolveTeve2Stream(callback: (ExtractorLink) -> Unit): Boolean = runCatching {
        val dynamicUrl = "$TEVE2_STREAM_URL?sid=${generateTeve2Sid()}&app=$TEVE2_APP_ID&ce=3"
        val response = app.get(
            dynamicUrl,
            headers = mapOf(
                "Origin" to TEVE2_ORIGIN,
                "User-Agent" to TEVE2_USER_AGENT,
            ),
            referer = TEVE2_REFERER,
        )

        if (!response.isSuccessful || !response.text.contains("#EXTM3U")) return@runCatching false

        val cookieHeader = response.headers.values("Set-Cookie").joinToString(";")
        val dix = Regex("(?:^|;\\s*)_dix=([^;]+)").find(cookieHeader)?.groupValues?.get(1)
            ?: return@runCatching false

        callback(
            newExtractorLink(
                source = name,
                name = "Teve2 • HLS",
                url = dynamicUrl,
                type = ExtractorLinkType.M3U8,
            ) {
                referer = TEVE2_REFERER
                quality = 360
                headers = mapOf(
                    "Origin" to TEVE2_ORIGIN,
                    "User-Agent" to TEVE2_USER_AGENT,
                    "Cookie" to "_dix=$dix",
                )
            },
        )
        true
    }.getOrDefault(false)

    private fun TvChannel.toSearchResponse() = newLiveSearchResponse(
        name = name,
        url = streamUrl,
        type = TvType.Live,
        fix = false,
    ) {
        posterUrl = logoUrl
        lang = "tr"
    }

    private fun isYouTubeUrl(url: String): Boolean =
        url.startsWith("https://www.youtube.com/") ||
            url.startsWith("https://youtube.com/") ||
            url.startsWith("https://www.youtube-nocookie.com/") ||
            url.startsWith("https://youtu.be/")
}

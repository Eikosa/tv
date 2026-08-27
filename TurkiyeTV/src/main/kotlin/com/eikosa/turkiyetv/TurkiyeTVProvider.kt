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
import com.lagradost.cloudstream3.utils.newExtractorLink

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

class TurkiyeTVProvider : MainAPI() {
    override var mainUrl = "https://github.com/Eikosa/tv"
    override var name = "Türkiye Türkçe Canlı TV"
    override var lang = "tr"
    override val supportedTypes = setOf(TvType.Live)
    override val hasMainPage = true
    override val mainPage = listOf(
        com.lagradost.cloudstream3.mainPage("featured", "Öne Çıkanlar"),
        com.lagradost.cloudstream3.mainPage("general", "Genel / Ulusal"),
        com.lagradost.cloudstream3.mainPage("news", "Haber"),
        com.lagradost.cloudstream3.mainPage("sports", "Spor"),
        com.lagradost.cloudstream3.mainPage("music", "Müzik"),
        com.lagradost.cloudstream3.mainPage("kids", "Çocuk / Eğitim"),
        com.lagradost.cloudstream3.mainPage("culture", "Belgesel / Kültür"),
        com.lagradost.cloudstream3.mainPage("religion", "Dini"),
        com.lagradost.cloudstream3.mainPage("entertainment", "Eğlence / Yaşam"),
        com.lagradost.cloudstream3.mainPage("local", "Yerel / Diğer"),
    )

    // Popüler ulusal ve haber kanalları önce, daha nadir/yerel kanallar sonra listelenir.
    // 98 statik yayın ve logo adresi 27 Ağustos 2026 tarihinde erişim testiyle doğrulandı.
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
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/f/f1/Logo_of_Show_TV.png",
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
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/72/TRT_Haber_Eyl%C3%BCl_2020_Logo.svg/960px-TRT_Haber_Eyl%C3%BCl_2020_Logo.svg.png",
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
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/09/TRT_Belgesel_logo_%282019-%29.svg/960px-TRT_Belgesel_logo_%282019-%29.svg.png",
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
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ef/GZT_logo.svg",
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
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val categorizedChannels = when (request.data) {
            "featured" -> channels.take(12)
            "general" -> channels.filter { it.group == "Genel" && it.channelNumber <= 32 }
            "news" -> channels.filter { it.group == "Haber" }
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
            "local" -> channels.filter {
                (it.group == "Genel" && it.channelNumber > 32) || it.group !in knownCategoryGroups
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
        name = "$channelNumber. $name",
        url = streamUrl,
        type = TvType.Live,
        fix = false,
    ) {
        posterUrl = logoUrl
        lang = "tr"
    }
}

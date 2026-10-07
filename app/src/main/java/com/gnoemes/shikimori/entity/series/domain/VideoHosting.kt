package com.gnoemes.shikimori.entity.series.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * A video hosting, as named in the translation lists. A hosting the app can play also has a
 * `HostingParser`, registered in `SeriesUtilModule`; every other one opens in the web player.
 */
sealed class VideoHosting : Parcelable {
    abstract val synonymType: String

    @Parcelize
    data class VK(
            override val synonymType: String = "vk.com"
    ) : VideoHosting()

    @Parcelize
    data class OK(
            override val synonymType: String = "ok.ru"
    ) : VideoHosting()

    @Parcelize
    data class ALLVIDEO(
            override val synonymType: String = "csst.online"
    ) : VideoHosting()

    @Parcelize
    data class ANIMEJOY(
            override val synonymType: String = "animejoy.ru"
    ) : VideoHosting()

    @Parcelize
    data class DZEN(
            override val synonymType: String = "dzen.ru"
    ) : VideoHosting()

    @Parcelize
    data class MAILRU(
            override val synonymType: String = "mail.ru"
    ) : VideoHosting()

    @Parcelize
    data class CDA(
            override val synonymType: String = "cda.pl"
    ) : VideoHosting()

    @Parcelize
    data class KODIK(
            override val synonymType: String = "kodikplayer.com"
    ) : VideoHosting()

    @Parcelize
    data class SIBNET(
            override val synonymType: String = "sibnet.ru"
    ) : VideoHosting()

    @Parcelize
    data class SOVET_ROMANTICA(
            override val synonymType: String = "sovetromantica.com"
    ) : VideoHosting()

    @Parcelize
    data class SMOTRET_ANIME(
            override val synonymType: String = "smotret-anime.online"
    ) : VideoHosting()

    @Parcelize
    data class MATRESHKA(
            override val synonymType: String = "matreshka.tv"
    ) : VideoHosting()

    @Parcelize
    data class UNKNOWN(
            override val synonymType: String = "unknown"
    ) : VideoHosting()

    companion object {

        /** The hosting a translation names. Anything not listed keeps its raw name as [UNKNOWN]. */
        fun fromName(raw: String?): VideoHosting {
            return when (raw) {
                "vk.com", "vk" -> VK()
                "ok.ru", "ok" -> OK()
                "csst.online", "www.csst.online", "fsst.online", "www.fsst.online", "secvideo1.online", "www.secvideo1.online", "dsst.online" -> ALLVIDEO()
                "animejoy.ru", "animejoya.ru", "animejoy.su" -> ANIMEJOY()
                "dzen.ru" -> DZEN()
                "my.mail.ru", "videoapi.my.mail.ru", "mail.ru" -> MAILRU()
                "ebd.cda.pl" -> CDA()
                "video.sibnet.ru", "sibnet", "sibnet.ru" -> SIBNET()
                "sovetromantica.com", "sovetromantica" -> SOVET_ROMANTICA()
                //anime365 keeps moving: .com -> .net (2024) -> .org (2024). All four still serve, and
                //links in shikimori's video db are spread across them, so keep every one of them here.
                "smotretanime.ru", "smotretanime", "smotret-anime.online", "smotret-anime.com", "smotret-anime.net", "smotret-anime.org" -> SMOTRET_ANIME()
                "aniqit.com", "kodikplayer.com" -> KODIK()
                "matreshka.tv" -> MATRESHKA()
                else -> UNKNOWN(raw ?: "unknown")
            }
        }
    }
}

package com.gnoemes.shikimori.entity.series.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.net.MalformedURLException
import java.net.URL

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

        /**
         * Dead domains whose players now serve on another domain, the path unchanged: Kodik's old ones
         * are gone from DNS, and the same paths on kodikplayer.com play (checked live 2026-10-16, 10-19,
         * 10-23). Shikicinema still hands them out (BUGS #25).
         */
        private val MOVED_HOSTS = mapOf(
                "aniqit.com" to "kodikplayer.com",
                "kodik.info" to "kodikplayer.com"
        )

        /**
         * [url] on its player's current domain when the one it names has moved ([MOVED_HOSTS]), else
         * [url] itself. Applied where Shikicinema's rows come in, so every player, the copied link,
         * sharing and the hosting filter all see the live address.
         */
        fun currentUrl(url: String): String {
            val host = try {
                URL(url).host
            } catch (e: MalformedURLException) {
                return url
            }
            val moved = MOVED_HOSTS[host] ?: return url
            return url.replaceFirst("//$host", "//$moved")
        }

        /** The hosting a translation names. Anything not listed keeps its raw name as [UNKNOWN]. */
        fun fromName(raw: String?): VideoHosting {
            return when (raw) {
                "vk.com", "vk" -> VK()
                "ok.ru", "ok" -> OK()
                "csst.online", "www.csst.online", "fsst.online", "www.fsst.online", "secvideo1.online", "www.secvideo1.online", "dsst.online" -> ALLVIDEO()
                //the same PlayerJS links on every one of them (BUGS #2)
                "animejoy.ru", "animejoya.ru", "animejoy.su", "anime-joy.online", "anime-joy.ru", "animejoy-2.ru" -> ANIMEJOY()
                "dzen.ru" -> DZEN()
                "my.mail.ru", "videoapi.my.mail.ru", "mail.ru" -> MAILRU()
                "ebd.cda.pl" -> CDA()
                "video.sibnet.ru", "sibnet", "sibnet.ru" -> SIBNET()
                "sovetromantica.com", "sovetromantica" -> SOVET_ROMANTICA()
                //anime365 keeps moving: .com -> .net (2024) -> .org (2024). All four still serve, and
                //links in shikimori's video db are spread across them, so keep every one of them here.
                "smotretanime.ru", "smotretanime", "smotret-anime.online", "smotret-anime.com", "smotret-anime.net", "smotret-anime.org" -> SMOTRET_ANIME()
                "kodikplayer.com" -> KODIK()
                "matreshka.tv" -> MATRESHKA()
                else -> UNKNOWN(raw ?: "unknown")
            }
        }
    }
}

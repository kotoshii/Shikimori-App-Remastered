package com.gnoemes.shikimori.utils

import android.graphics.Bitmap
import com.gnoemes.shikimori.entity.app.domain.Constants
import android.webkit.CookieManager
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.domain.VideoHosting


object Utils {

    fun hostingFromString(raw: String?): VideoHosting {
        return when (raw) {
            "vk.com", "vk" -> VideoHosting.VK()
            "ok.ru", "ok" -> VideoHosting.OK()
            "www.myvi.top", "www.myvi.tv", "myvi.top", "myvi.tv" -> VideoHosting.MYVI()
            "csst.online", "www.csst.online", "fsst.online", "www.fsst.online", "secvideo1.online", "www.secvideo1.online", "dsst.online" -> VideoHosting.ALLVIDEO()
            "animejoy.ru", "animejoya.ru", "animejoy.su" -> VideoHosting.ANIMEJOY()
            "dzen.ru" -> VideoHosting.DZEN()
            "nuum.ru" -> VideoHosting.NUUM()
            "my.mail.ru", "videoapi.my.mail.ru", "mail.ru" -> VideoHosting.MAILRU()
            "ebd.cda.pl" -> VideoHosting.CDA()
            "video.sibnet.ru", "sibnet", "sibnet.ru" -> VideoHosting.SIBNET()
            "sovetromantica.com", "sovetromantica" -> VideoHosting.SOVET_ROMANTICA()
            //anime365 keeps moving: .com -> .net (2024) -> .org (2024). All four still serve, and
            //links in shikimori's video db are spread across them, so keep every one of them here.
            "smotretanime.ru", "smotretanime", "smotret-anime.online", "smotret-anime.com", "smotret-anime.net", "smotret-anime.org" -> VideoHosting.SMOTRET_ANIME()
            "aniqit.com", "kodikplayer.com" -> VideoHosting.KODIK()
            else -> (raw ?: "unknown").let { hosting -> VideoHosting.UNKNOWN(hosting, hosting) }
        }
    }

    /**
     * Whether the app can resolve real tracks for a hosting, and so offer the embedded player,
     * a quality menu and downloading.
     *
     * SMOTRET_ANIME is here because Anime365Parser resolves it client side. It only produces tracks
     * for a user with an anime365 token *and* a paid subscription; everyone else gets an empty
     * track list and falls back to the web player, which appends the same token.
     */
    fun isHostingSupports(hosting: VideoHosting): Boolean {
        return when (hosting) {
            is VideoHosting.SIBNET, is VideoHosting.VK, is VideoHosting.SMOTRET_ANIME, is VideoHosting.SOVET_ROMANTICA, is VideoHosting.KODIK, is VideoHosting.OK, is VideoHosting.MYVI, is VideoHosting.ALLVIDEO, is VideoHosting.ANIMEJOY, is VideoHosting.DZEN, is VideoHosting.NUUM, is VideoHosting.MAILRU, is VideoHosting.CDA -> true
            else -> false
        }
    }

    fun getRequestHeadersForHosting(video: Video?): Map<String, String> = when (video?.hosting) {
        is VideoHosting.SOVET_ROMANTICA, is VideoHosting.UNKNOWN -> mapOf(Pair("Referrer", video.player))
        is VideoHosting.SIBNET -> mapOf(Pair("Referer", video.player))
        is VideoHosting.MAILRU -> mapOf(Pair("Cookie", CookieManager.getInstance().getCookie(".my.mail.ru")))
        is VideoHosting.NUUM -> mapOf(Pair("Referer", "https://nuum.ru/"))
        is VideoHosting.DZEN -> mapOf(Pair("User-Agent", Constants.PLAYER_USER_AGENT))
        else -> emptyMap()
    }

    fun getDominantColor(bitmap: Bitmap): Int {
        val newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val color = newBitmap.getPixel(0, 0)
        newBitmap.recycle()
        return color
    }

    fun checkNeedIFrame(url: String): Boolean {
        return when {
            url.contains("aparat") -> false
            else -> true
        }
    }

}
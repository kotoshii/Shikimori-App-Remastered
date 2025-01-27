package com.gnoemes.shikimori.data.repository.series.shikimori.converter

import android.webkit.CookieManager
import com.gnoemes.shikimori.entity.series.data.MailRuVideoResponse
import com.gnoemes.shikimori.entity.series.data.MailRuVideosResponse
import com.gnoemes.shikimori.entity.series.data.MailRuPlayerDataJsonModel
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.google.gson.Gson
import okhttp3.Response
import org.jsoup.Jsoup
import java.net.HttpCookie
import javax.inject.Inject

class MailRuVideoConverterImpl @Inject constructor() : MailRuVideoConverter {

    override fun convertTracks(it: TranslationVideo, videos: List<MailRuVideoResponse>): Video {
        val tracks = videos.map { Track(it.key.replace("p", ""), if (it.url.startsWith("http")) it.url else "https:${it.url}") }
        return Video(it.animeId, it.episodeIndex.toLong(), it.webPlayerUrl!!, it.videoHosting, tracks, null, null)
    }

    override fun parseVideoMetaUrl(html: String?): String? {
        if (html.isNullOrEmpty()) return null
        val doc = Jsoup.parse(html)
        val playerDataJson = doc.select("script:containsData(flashVars):containsData(video):containsData(metadataUrl)")
                .first()
                .data()
        if (playerDataJson.isNullOrEmpty()) return null

        val gson = Gson()
        val playerData = gson.fromJson<MailRuPlayerDataJsonModel>(playerDataJson, MailRuPlayerDataJsonModel::class.java)

        return "https://my.mail.ru${playerData.video.metadataUrl}"
    }

    override fun parsePlaylists(videosMetadata: MailRuVideosResponse?): List<MailRuVideoResponse> {
        if (videosMetadata == null) return listOf()
        return videosMetadata.videos
    }

    override fun saveCookies(response: Response) {
        val cookies = HttpCookie.parse(response.header("Set-Cookie"))
        val videoKeyCookie = cookies.find { it.name == "video_key" }

        if (videoKeyCookie != null) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setCookie(videoKeyCookie.domain, videoKeyCookie.toString())
        }
    }
}
package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import android.webkit.CookieManager
import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.entity.series.data.MailRuVideosResponse
import com.gnoemes.shikimori.entity.series.data.MailRuPlayerData
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import com.google.gson.Gson
import io.reactivex.Single
import org.jsoup.Jsoup
import retrofit2.Response
import java.net.HttpCookie
import javax.inject.Inject

class MailRuParser @Inject constructor(
        private val api: VideoApi
) : HostingParser {

    override val hosting = VideoHosting.MAILRU::class.java

    override fun parse(playerUrl: String): Single<ParsedVideo> =
            api.getPlayerHtml(playerUrl)
                    .flatMap { html ->
                        val metaUrl = parseVideoMetaUrl(html.string())
                                ?: return@flatMap Single.just(ParsedVideo.EMPTY)

                        api.getMailRuVideoMeta(metaUrl)
                                .map { saveCookies(it) }
                                .map { ParsedVideo(tracks(it.body())) }
                    }

    /** The streams want the `video_key` cookie the metadata call set, see [saveCookies]. */
    override fun headers(video: Video): Map<String, String> =
            CookieManager.getInstance().getCookie(".my.mail.ru")
                    ?.let { mapOf("Cookie" to it) }
                    .orEmpty()

    private fun parseVideoMetaUrl(html: String?): String? {
        if (html.isNullOrEmpty()) return null

        val doc = Jsoup.parse(html)
        val playerDataJson = doc
                .select("script:containsData(flashVars):containsData(video):containsData(metadataUrl)")
                .first()
                ?.data()

        if (playerDataJson.isNullOrEmpty()) return null

        val gson = Gson()
        val playerData = gson.fromJson<MailRuPlayerData>(playerDataJson, MailRuPlayerData::class.java)

        return "https://my.mail.ru${playerData.video.metadataUrl}"
    }

    private fun tracks(videosMetadata: MailRuVideosResponse?): List<Track> {
        return videosMetadata
                ?.videos
                ?.map {
                    val quality = it.key.replace("p", "")
                    val url = if (it.url.startsWith("http")) it.url else "https:${it.url}"

                    Track(quality, url)
                }
                ?.sortedByDescending { it.quality.toInt() }
                .orEmpty()
    }

    private fun saveCookies(response: Response<MailRuVideosResponse>): Response<MailRuVideosResponse> {
        val cookies = HttpCookie.parse(response.raw().header("Set-Cookie"))
        val videoKeyCookie = cookies.find { it.name == "video_key" }

        if (videoKeyCookie != null) {
            CookieManager.getInstance().setCookie(videoKeyCookie.domain, videoKeyCookie.toString())
        }

        return response
    }
}
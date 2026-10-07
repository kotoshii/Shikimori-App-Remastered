package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import io.reactivex.Single
import org.jsoup.Jsoup
import javax.inject.Inject

class SibnetParser @Inject constructor(
        private val api: VideoApi
) : HostingParser {

    override val hosting = VideoHosting.SIBNET::class.java

    override fun parse(playerUrl: String): Single<ParsedVideo> =
            api.getPlayerHtml(playerUrl).map { ParsedVideo(tracks(it.string())) }

    override fun headers(video: Video): Map<String, String> = mapOf("Referer" to video.player)

    private fun tracks(html: String?): List<Track> {
        if (html.isNullOrEmpty()) return emptyList()

        val doc = Jsoup.parse(html)
        val scriptData = doc.select("script[type=\"text/javascript\"]:containsData(player)").first()?.data() ?: return emptyList()

        val regex = Regex("player.src.+?(\".+?\")")

        val playlistUrl = regex
                .find(scriptData)
                ?.groupValues
                ?.getOrNull(1)
                ?.replace("\"", "")
                ?.let { if (it.firstOrNull() == '/') it else "/$it" }
                ?.let { "https://video.sibnet.ru$it" }

        return if (playlistUrl != null) listOf(Track("unknown", playlistUrl)) else emptyList()
    }
}
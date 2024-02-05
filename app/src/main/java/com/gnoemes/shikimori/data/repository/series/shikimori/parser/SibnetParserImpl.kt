package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import org.jsoup.Jsoup
import javax.inject.Inject

class SibnetParserImpl @Inject constructor() : SibnetParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    override fun tracks(html: String?): List<Track> {
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
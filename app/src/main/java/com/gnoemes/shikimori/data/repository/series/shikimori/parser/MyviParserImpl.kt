package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import org.jsoup.Jsoup
import javax.inject.Inject

class MyviParserImpl @Inject constructor() : MyviParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    override fun tracks(html: String?): List<Track> {
        if (html.isNullOrEmpty()) return emptyList()

        val doc = Jsoup.parse(html)
        val scriptString = doc.select("script").find { it.data().contains("CreatePlayer(\"v") }?.data()?.toString()

        val playlistUrl = scriptString
                ?.substringAfter("\"v=")
                ?.substringBefore("\\u0026tp=video")
                ?.replace("%26", "&")
                ?.replace("%3a", ":")
                ?.replace("%2f", "/")
                ?.replace("%3f", "?")
                ?.replace("%3d", "=")
                ?.let { if (it.startsWith("http")) it else "https:$it" }

        return if (playlistUrl != null) listOf(Track("unknown", playlistUrl)) else emptyList()
    }
}
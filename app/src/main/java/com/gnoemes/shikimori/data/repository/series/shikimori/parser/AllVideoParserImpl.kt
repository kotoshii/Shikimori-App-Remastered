package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import org.jsoup.Jsoup
import javax.inject.Inject

class AllVideoParserImpl @Inject constructor() : AllVideoParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    override fun tracks(html: String?): List<Track> {
        if (html.isNullOrEmpty()) return emptyList()

        val doc = Jsoup.parse(html)
        val scriptData = doc.select("script:containsData(isMobile):containsData(file:)").first()?.data()

        return scriptData
                ?.substringAfter("file:\"")
                ?.substringBefore('"')
                ?.split(",")
                .orEmpty()
                .map {
                    val match = Regex("\\[(\\d+)p\\](.+)").find(it)

                    if (match == null) {
                        Track("unknown", it)
                    } else {
                        val (quality, url) = match.destructured
                        Track(quality, url)
                    }
                }
                .sortedByDescending { it.quality.toInt() }
    }
}
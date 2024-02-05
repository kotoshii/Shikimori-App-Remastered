package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.utils.toUri
import javax.inject.Inject

class AnimeJoyParserImpl @Inject constructor() : AnimeJoyParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    override fun tracks(embedUrl: String?): List<Track> {
        return embedUrl
                ?.toUri()
                ?.getQueryParameter("file")
                ?.split(",")
                .orEmpty()
                .map {
                    val quality = it
                            .substringAfter("[")
                            .substringBefore("p]")
                    val url = it
                            .substringAfter("]")
                    Track(quality, url)
                }
                .sortedByDescending { it.quality.toInt() }
    }
}
package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import javax.inject.Inject

class VkParserImpl @Inject constructor() : VkParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    override fun tracks(html: String?): List<Track> {
        if (html.isNullOrEmpty()) return emptyList()

        val regex = Regex("\"(mp4_144|mp4_240|mp4_360|mp4_480|mp4_720|mp4_1080)\":\\s?\"(.*?)\"")
        val matches = regex.findAll(html)

        return matches
                .map { it.destructured.toList() }
                .map {
                    val (key, value) = it
                    Track(key.replace("mp4_", ""), value)
                }
                .toList()
                .sortedByDescending { it.quality.toInt() }
    }
}
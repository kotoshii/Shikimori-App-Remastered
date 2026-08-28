package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.data.anime365.Anime365VideoResponse
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo

interface Anime365Parser {

    fun video(video: TranslationVideo, tracks: List<Track>, subtitles: String?): Video

    fun apiUrl(playerUrl: String, token: String?): String?

    fun tracks(response: Anime365VideoResponse?): List<Track>

    fun subtitles(response: Anime365VideoResponse?, playerUrl: String): String?
}

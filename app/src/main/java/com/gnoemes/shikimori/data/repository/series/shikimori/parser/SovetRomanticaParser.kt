package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo

interface SovetRomanticaParser {

    fun video(video: TranslationVideo, tracks: List<Track>): Video

    fun tracks(m3uContent: String?, masterPlaylistUrl: String?): List<Track>

    fun getMasterPlaylistUrl(html: String?): String?
}
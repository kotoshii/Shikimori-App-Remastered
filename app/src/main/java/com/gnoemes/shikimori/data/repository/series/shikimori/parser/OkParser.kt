package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo

interface OkParser {

    fun video(video: TranslationVideo, tracks: List<Track>): Video

    /** The progressive files, which ok.ru has cut down to one rendition. */
    fun tracks(html: String?): List<Track>

    /** ok.ru's own master playlist, or null when it offers none and the progressive list is all. */
    fun getMasterPlaylistUrl(html: String?): String?

    /** The full quality ladder, read from the master playlist. */
    fun tracks(m3uContent: String?, masterPlaylistUrl: String?): List<Track>
}

package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo

interface CdaParser {

    fun video(video: TranslationVideo, tracks: List<Track>): Video

    fun getManifestUrl(html: String?): String?

    fun tracks(manifestXml: String?, manifestUrl: String?): List<Track>
}

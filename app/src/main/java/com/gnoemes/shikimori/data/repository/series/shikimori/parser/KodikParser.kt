package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.data.kodik.KodikLinksResponse
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo

interface KodikParser {

    fun video(video: TranslationVideo, tracks: List<Track>): Video

    fun linkRequestParams(html: String?): Map<String, String>

    fun linksUrl(playerUrl: String): String?

    fun playerScriptUrl(html: String?, playerUrl: String): String?

    fun rememberLinksUrl(playerScript: String?, playerUrl: String): String?

    fun tracks(response: KodikLinksResponse?): List<Track>
}

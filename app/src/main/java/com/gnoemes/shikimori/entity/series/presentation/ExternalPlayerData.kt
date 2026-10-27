package com.gnoemes.shikimori.entity.series.presentation

import com.gnoemes.shikimori.entity.series.domain.Track

/**
 * What «Внешний плеер» opens: [url] with its [mimeType] - Android offers only the apps that declare
 * that type, and a player told mp4 for an HLS playlist did not play it (BUGS #29) - and the [headers]
 * the built-in player would send, which MX Player reads.
 */
data class ExternalPlayerData(
        val url: String,
        val mimeType: String,
        val headers: Map<String, String>
) {

    companion object {
        //lower case: Android matches types case-sensitively (IntentFilter)
        private const val HLS = "application/vnd.apple.mpegurl"
        private const val MP4 = "video/mp4"

        fun of(track: Track, headers: Map<String, String>): ExternalPlayerData =
                ExternalPlayerData(track.url, if (track.isHls) HLS else MP4, headers)
    }
}

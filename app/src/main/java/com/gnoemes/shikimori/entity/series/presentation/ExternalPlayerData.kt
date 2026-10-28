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
        val headers: Map<String, String>,
        //a DASH manifest: the sound plays only in a player that reads one
        val isDash: Boolean = false
) {

    companion object {
        //lower case: Android matches types case-sensitively (IntentFilter)
        private const val HLS = "application/vnd.apple.mpegurl"
        private const val MP4 = "video/mp4"
        //no app declares application/dash+xml - VLC, which plays DASH, does not either, so nothing was
        //offered - and VLC takes any video (checked on the emulator, P7-T5 Test 98g)
        private const val ANY_VIDEO = "video/*"

        /** A track whose sound is its own file goes as its manifest, which holds every quality and the sound (BUGS #56). */
        fun of(track: Track, headers: Map<String, String>): ExternalPlayerData = when {
            track.manifestUrl != null -> ExternalPlayerData(track.manifestUrl, ANY_VIDEO, headers, isDash = true)
            track.isHls -> ExternalPlayerData(track.url, HLS, headers)
            else -> ExternalPlayerData(track.url, MP4, headers)
        }
    }
}

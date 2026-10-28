package com.gnoemes.shikimori.entity.series.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Track(
        val quality : String,
        val url : String,
        //set when the hosting serves sound as its own file, see CdaParser
        val audioUrl : String? = null,
        //set with audioUrl: the DASH manifest that lists every quality and the sound, the one url an
        //outside player can play this video with sound from (BUGS #56)
        val manifestUrl : String? = null
) : Parcelable {

    /** False when the hosting did not tell the quality: blank, or [UNKNOWN_QUALITY]. */
    val isQualityKnown: Boolean
        get() = quality.isNotBlank() && quality != UNKNOWN_QUALITY

    /** «720p»; null when the quality is not known. */
    val displayQuality: String?
        get() = if (isQualityKnown) "${quality}p" else null

    /** An HLS playlist rather than a file. */
    val isHls: Boolean
        get() = isHls(url)

    companion object {
        /** A parser's quality for a hosting that does not say it. */
        const val UNKNOWN_QUALITY = "unknown"

        /**
         * Whether [url] is an HLS playlist rather than a file: the one rule for the built-in player, the
         * downloads and outside players, which had three of their own (P7-T5 14.1).
         */
        fun isHls(url: String): Boolean = url.contains("m3u8", ignoreCase = true)
    }
}

/**
 * Best quality first, a track whose quality is not a number last: every parser sorts with this. Five
 * of them sorted with `toInt()`, which throws on such a track as soon as there are two (BUGS #1, #24).
 */
fun List<Track>.sortedByQuality(): List<Track> = sortedByDescending { it.quality.toIntOrNull() ?: -1 }
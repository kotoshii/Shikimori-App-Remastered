package com.gnoemes.shikimori.entity.series.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Track(
        val quality : String,
        val url : String,
        //set when the hosting serves sound as its own file, see CdaParser
        val audioUrl : String? = null
) : Parcelable {

    /** False when the hosting did not tell the quality: blank, or [UNKNOWN_QUALITY]. */
    val isQualityKnown: Boolean
        get() = quality.isNotBlank() && quality != UNKNOWN_QUALITY

    /** «720p»; null when the quality is not known. */
    val displayQuality: String?
        get() = if (isQualityKnown) "${quality}p" else null

    companion object {
        /** A parser's quality for a hosting that does not say it. */
        const val UNKNOWN_QUALITY = "unknown"
    }
}

/**
 * Best quality first, a track whose quality is not a number last: every parser sorts with this. Five
 * of them sorted with `toInt()`, which throws on such a track as soon as there are two (BUGS #1, #24).
 */
fun List<Track>.sortedByQuality(): List<Track> = sortedByDescending { it.quality.toIntOrNull() ?: -1 }
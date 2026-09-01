package com.gnoemes.shikimori.entity.series.data

import com.google.gson.annotations.SerializedName

/**
 * The base64'd path segment sitting in the middle of every matreshka stream url, which describes
 * what that url serves: `{"id":"8AJAKMnsyAQ","codec":"h264","userQuality":[720]}`.
 *
 * An empty quality list means the adaptive playlist rather than one quality. Matreshka's own api
 * spells the same field `height`, so both names are read.
 */
data class MatreshkaPlaylistData(
        @SerializedName("codec") val codec: String?,
        @SerializedName("userQuality") val userQuality: List<Int>?,
        @SerializedName("height") val height: List<Int>?
) {

    /** The single quality this url serves, or null when it is adaptive or names several. */
    val quality: Int?
        get() = (userQuality ?: height)?.singleOrNull()
}

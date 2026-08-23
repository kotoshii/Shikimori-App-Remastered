package com.gnoemes.shikimori.entity.download

import com.google.gson.annotations.SerializedName

/**
 * A video download and its separate audio download waiting to be joined once both finish.
 *
 * Kept in preferences rather than memory because the download manager can finish long after the
 * app was closed.
 */
data class PendingMux(
        @SerializedName("videoId") val videoId: Long,
        @SerializedName("audioId") val audioId: Long,
        @SerializedName("outputPath") val outputPath: String
) {
    fun owns(downloadId: Long) = downloadId == videoId || downloadId == audioId
}

package com.gnoemes.shikimori.entity.series.data

import com.google.gson.annotations.SerializedName

/**
 * Shape of the `var _params=({...})` block on a dzen embed page.
 */
data class DzenPlayerData(
        @SerializedName("ssrData") val ssrData: SsrData?
) {
    data class SsrData(
            @SerializedName("exportResponse") val exportResponse: ExportResponse?
    ) {
        data class ExportResponse(
                @SerializedName("content") val content: Content?
        ) {
            data class Content(
                    @SerializedName("streams") val streams: List<Stream>?
            ) {
                data class Stream(
                        @SerializedName("url") val url: String?,
                        @SerializedName("type") val type: String?
                )
            }
        }
    }
}

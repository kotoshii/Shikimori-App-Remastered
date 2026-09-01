package com.gnoemes.shikimori.entity.series.data.anime365

import com.google.gson.annotations.SerializedName

/**
 * Answer of `translations/embed/{id}?access_token=`, the one authenticated call anime365 needs to
 * hand over stream links. `stream` is the quality list - one entry per height, each with its own
 * urls.
 *
 * ⚠️ anime365 reports failures as **HTTP 200 with an `error` object** instead of an error status,
 * so `data` is null and `error` is set on "not logged in" and "no subscription" alike. Everything
 * here is nullable for that reason - check [isError] rather than trusting the status code.
 */
data class Anime365VideoResponse(
        @SerializedName("data") val data: Data?,
        @SerializedName("error") val error: Error?
) {

    val isError: Boolean
        get() = error != null || data == null

    data class Data(
            @SerializedName("embedUrl") val embedUrl: String?,
            @SerializedName("stream") val stream: List<Stream>?,
            @SerializedName("subtitlesUrl") val subtitlesUrl: String?
    ) {
        data class Stream(
                @SerializedName("height") val height: Int?,
                //elements are nullable on purpose - gson will happily put a null in the list
                @SerializedName("urls") val urls: List<String?>?
        )
    }

    data class Error(
            @SerializedName("code") val code: Int?,
            @SerializedName("message") val message: String?
    )
}

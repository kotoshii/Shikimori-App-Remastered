package com.gnoemes.shikimori.entity.series.data

import com.gnoemes.shikimori.entity.series.data.shikicinema.ShikicinemaTranslationResponse
import com.gnoemes.shikimori.entity.series.domain.TranslationQuality
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import com.google.gson.annotations.SerializedName

data class TranslationResponse(
        @field:SerializedName("id") val id: Long,
        @field:SerializedName("animeId") val animeId: Long,
        @field:SerializedName("episodeId") val episodeId: Int,
        @field:SerializedName("type") private val _type: TranslationType?,
        @field:SerializedName("quality") val quality: TranslationQuality,
        @field:SerializedName("hosting") val _hosting: String?,
        @field:SerializedName("author") val author: String,
        @field:SerializedName("episodesSize") val episodesSize: Int,
        val webPlayerUrl: String? = null
) {

    //animeId is the id the translations were requested for, not the one in the response - the
    //response's key has been renamed under us before, and a missing key silently parses as 0
    constructor(response: ShikicinemaTranslationResponse, animeId: Long, episodesSize: Int) : this(
            response.id,
            animeId,
            response.episode,
            response.kind,
            response.quality,
            response.hosting,
            response.author ?: "",
            episodesSize,
            response.url
    )

    val hosting: VideoHosting
        get() = VideoHosting.fromName(_hosting)

    val type: TranslationType
        get() = _type ?: TranslationType.VOICE_RU
}
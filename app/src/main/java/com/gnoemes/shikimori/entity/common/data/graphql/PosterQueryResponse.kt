package com.gnoemes.shikimori.entity.common.data.graphql

import com.google.gson.annotations.SerializedName

/**
 * Answer to the batched poster query. Every root field is optional - only the ones actually
 * asked for in a particular batch are present.
 */
data class PosterQueryResponse(
        @field:SerializedName("data") val data: PosterQueryData?
)

data class PosterQueryData(
        @field:SerializedName("animes") val animes: List<PosterHolder>?,
        @field:SerializedName("mangas") val mangas: List<PosterHolder>?,
        @field:SerializedName("characters") val characters: List<PosterHolder>?,
        @field:SerializedName("people") val people: List<PosterHolder>?
)

data class PosterHolder(
        @field:SerializedName("id") val id: String?,
        @field:SerializedName("poster") val poster: PosterResponse?
)

package com.gnoemes.shikimori.entity.common.data.graphql

import com.google.gson.annotations.SerializedName

/**
 * Answer to the per-title genre query, e.g.
 * `{animes(ids:"31240",limit:1,censored:false){id genres{id name russian kind}}}`.
 *
 * Only the root field that was asked for is present, exactly like [PosterQueryResponse].
 */
data class GenreQueryResponse(
        @field:SerializedName("data") val data: GenreQueryData?
)

data class GenreQueryData(
        @field:SerializedName("animes") val animes: List<GenreHolder>?,
        @field:SerializedName("mangas") val mangas: List<GenreHolder>?
)

data class GenreHolder(
        @field:SerializedName("id") val id: String?,
        @field:SerializedName("genres") val genres: List<GenreResponseV2>?
)

/**
 * `id` is a graphql `ID!`, which is a **string** on the wire even though it holds a number.
 */
data class GenreResponseV2(
        @field:SerializedName("id") val id: String?,
        @field:SerializedName("name") val name: String?,
        @field:SerializedName("russian") val russian: String?,
        @field:SerializedName("kind") val kind: String?
)

package com.gnoemes.shikimori.entity.common.data.graphql

import com.google.gson.annotations.SerializedName

/**
 * Answer to the full genre list query, both entry types in one request through aliases:
 * `{anime: genres(entryType: Anime){...} manga: genres(entryType: Manga){...}}`.
 *
 * The field names here are the **aliases**, not root queries - `genres` takes an entry type, so the
 * two lists cannot be told apart any other way.
 */
data class GenreVocabularyResponse(
        @field:SerializedName("data") val data: GenreVocabularyData?
)

data class GenreVocabularyData(
        @field:SerializedName("anime") val anime: List<GenreResponseV2>?,
        @field:SerializedName("manga") val manga: List<GenreResponseV2>?
)

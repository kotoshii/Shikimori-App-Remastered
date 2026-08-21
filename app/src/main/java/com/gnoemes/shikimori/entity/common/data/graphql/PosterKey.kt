package com.gnoemes.shikimori.entity.common.data.graphql

/**
 * Content types whose posters can be recovered through GraphQL.
 *
 * Ranobe are mangas as far as Shikimori is concerned - they share the `mangas` root query and
 * even store their posters under `/uploads/poster/mangas/`, so they map to [MANGA].
 *
 * Clubs, studios and publishers have no GraphQL root query at all and therefore cannot be fixed.
 */
enum class PosterEntityType {
    ANIME,
    MANGA,
    CHARACTER,
    PERSON
}

data class PosterKey(
        val type: PosterEntityType,
        val id: Long
)

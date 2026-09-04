package com.gnoemes.shikimori.entity.common.data.graphql

/**
 * Which graphql root query holds the genres of a title.
 *
 * Ranobe are [MANGA] here - graphql has **no** ranobe root query, and `mangas(kind:"light_novel,
 * novel")` reproduces `/api/ranobe` exactly (verified 2026-09-02).
 */
enum class GenreEntryType(val rootField: String) {
    ANIME("animes"),
    MANGA("mangas")
}

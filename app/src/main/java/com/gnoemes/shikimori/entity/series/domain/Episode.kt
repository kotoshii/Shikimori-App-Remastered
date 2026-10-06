package com.gnoemes.shikimori.entity.series.domain

data class Episode(
        val id: Long,
        val index: Int,
        val animeId: Long,
        val isWatched: Boolean
)
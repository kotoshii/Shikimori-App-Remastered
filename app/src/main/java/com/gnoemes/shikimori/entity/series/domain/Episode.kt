package com.gnoemes.shikimori.entity.series.domain

data class Episode(
        val id: Long,
        val index: Int,
        val animeId: Long,
        //set from shikimori's watched count, see SeriesInteractorImpl.getEpisodes
        val isWatched: Boolean = false
)
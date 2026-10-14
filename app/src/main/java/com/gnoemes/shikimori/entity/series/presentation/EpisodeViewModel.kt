package com.gnoemes.shikimori.entity.series.presentation

data class EpisodeViewModel(
        val id: Long,
        val index: Int,
        val isWatched: Boolean,
        val isOpened : Boolean,
        val isGuest : Boolean
)
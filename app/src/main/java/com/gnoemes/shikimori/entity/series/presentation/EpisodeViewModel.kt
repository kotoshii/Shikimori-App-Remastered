package com.gnoemes.shikimori.entity.series.presentation

data class EpisodeViewModel(
        val id: Long,
        val index: Int,
        val animeId: Long,
        val state: State,
        val isWatched: Boolean,
        val isOpened : Boolean,
        val isGuest : Boolean
) {

    sealed class State {
        object NotChecked : State()
        object Loading : State()
        object Checked : State()
    }
}
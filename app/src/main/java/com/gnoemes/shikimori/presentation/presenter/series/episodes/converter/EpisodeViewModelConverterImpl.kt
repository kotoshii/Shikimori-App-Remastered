package com.gnoemes.shikimori.presentation.presenter.series.episodes.converter

import com.gnoemes.shikimori.entity.series.domain.Episode
import com.gnoemes.shikimori.entity.series.presentation.EpisodeViewModel
import javax.inject.Inject

class EpisodeViewModelConverterImpl @Inject constructor() : EpisodeViewModelConverter {

    override fun convert(t: List<Episode>, currentEpisode: Int, isGuest: Boolean): List<EpisodeViewModel> =
            t.asSequence()
                    .map { convertEpisode(it, currentEpisode, isGuest) }
                    .sortedBy { it.index }
                    .toMutableList()


    private fun convertEpisode(it: Episode, currentEpisode: Int, isGuest: Boolean): EpisodeViewModel {
        return EpisodeViewModel(
                it.id,
                it.index,
                it.isWatched,
                it.index == currentEpisode,
                isGuest
        )
    }
}
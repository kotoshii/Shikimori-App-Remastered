package com.gnoemes.shikimori.presentation.presenter.similar.converter

import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.anime.domain.AnimeWithStatus
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.manga.domain.MangaWithStatus
import com.gnoemes.shikimori.entity.similar.presentation.SimilarViewModel
import com.gnoemes.shikimori.presentation.presenter.common.converter.TitleTextConverter
import javax.inject.Inject

class SimilarViewModelConverterImpl @Inject constructor(
        private val settings: SettingsSource,
        private val titleText: TitleTextConverter
) : SimilarViewModelConverter {

    override fun apply(t: List<Any>, isGuest: Boolean): List<SimilarViewModel> = t.mapNotNull {
        when (it) {
            is AnimeWithStatus -> convertAnime(it, isGuest)
            is MangaWithStatus -> convertManga(it, isGuest)
            else -> null
        }
    }

    private fun convertAnime(it: AnimeWithStatus, isGuest: Boolean): SimilarViewModel {
        return SimilarViewModel(
                it.anime.id,
                it.rateId ?: Constants.NO_ID,
                it.anime.linkedType,
                settings.titleName(it.anime.name, it.anime.nameRu),
                titleText.describe(it.anime),
                it.anime.image,
                it.anime.score,
                it.status,
                isGuest
        )
    }

    private fun convertManga(it: MangaWithStatus, isGuest: Boolean): SimilarViewModel {
        return SimilarViewModel(
                it.manga.id,
                it.rateId ?: Constants.NO_ID,
                it.manga.linkedType,
                settings.titleName(it.manga.name, it.manga.nameRu),
                titleText.describe(it.manga),
                it.manga.image,
                it.manga.score,
                it.status,
                isGuest
        )
    }
}

package com.gnoemes.shikimori.presentation.presenter.rates.converter

import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.PinnedRate
import com.gnoemes.shikimori.entity.rates.domain.Rate
import com.gnoemes.shikimori.entity.rates.presentation.RateViewModel
import com.gnoemes.shikimori.presentation.presenter.common.converter.TitleTextConverter
import javax.inject.Inject

class RateViewModelConverterImpl @Inject constructor(
        private val settings: SettingsSource,
        private val titleText: TitleTextConverter
) : RateViewModelConverter {

    override fun apply(rates: List<Rate>, pinned: List<PinnedRate>): List<RateViewModel> =
            rates.map { convertRate(it, pinned) }
                    .sortedBy { !it.isPinned }

    private fun convertRate(it: Rate, pinned: List<PinnedRate>): RateViewModel {
        val type = if (it.manga == null) Type.ANIME else Type.MANGA

        fun isAnime(): Boolean = type == Type.ANIME

        val contentId = if (isAnime()) it.anime!!.id else it.manga!!.id
        val image = if (isAnime()) it.anime!!.image else it.manga!!.image
        val name = if (isAnime()) settings.titleName(it.anime!!.name, it.anime.nameRu)
        else settings.titleName(it.manga!!.name, it.manga.nameRu)

        val description = if (isAnime()) titleText.describe(it.anime!!) else titleText.describe(it.manga!!)
        val progress = if (isAnime()) it.episodes.invalidIfNullOrZero() else it.chapters.invalidIfNullOrZero()

        val pinnedRate = pinned.find { contentId == it.id }
        val isPinned = pinnedRate != null

        return RateViewModel(
                it.id,
                contentId,
                type,
                image,
                name,
                description,
                it.score.invalidIfNullOrZero(),
                progress,
                it,
                isPinned,
                pinnedRate?.order ?: Constants.MAX_PINNED_RATES + 1
        )
    }

    private fun Int?.invalidIfNullOrZero(): String = if (this == null || this == 0) "-" else this.toString()
}

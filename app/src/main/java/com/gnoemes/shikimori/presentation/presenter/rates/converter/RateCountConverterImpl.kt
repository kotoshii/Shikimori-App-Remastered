package com.gnoemes.shikimori.presentation.presenter.rates.converter

import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.presentation.RateCategory
import com.gnoemes.shikimori.entity.user.domain.Status
import com.gnoemes.shikimori.entity.user.domain.UserDetails
import com.gnoemes.shikimori.presentation.presenter.rates.provider.RateResourceProvider
import javax.inject.Inject

class RateCountConverterImpl @Inject constructor(
        private val resourceProvider: RateResourceProvider
) : RateCountConverter {

    override fun countAnimeRates(t: UserDetails): List<RateCategory> {
        return if (!t.stats.hasAnime) emptyList()
        else countRates(t.stats.animeStatuses, isAnime = true)
    }

    override fun countMangaRates(t: UserDetails): List<RateCategory> {
        return if (!t.stats.hasManga) emptyList()
        else countRates(t.stats.mangaStatuses, isAnime = false)
    }

    private fun countRates(statuses: List<Status>?, isAnime: Boolean): List<RateCategory> =
            RateStatus.values().mapNotNull { status ->
                val count = countRateSize(statuses, status)
                if (count > 0) RateCategory(status, resourceProvider.getRateStatusName(status, isAnime), count)
                else null
            }

    private fun countRateSize(statuses: List<Status>?, rateStatus: RateStatus): Int {
        return statuses?.asSequence()?.filter { it.name == rateStatus }?.sumBy { it.size }
                ?: 0
    }

}
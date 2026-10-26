package com.gnoemes.shikimori.presentation.presenter.rates.converter

import com.gnoemes.shikimori.entity.rates.domain.PinnedRate
import com.gnoemes.shikimori.entity.rates.domain.Rate
import com.gnoemes.shikimori.entity.rates.presentation.RateViewModel

interface RateViewModelConverter {

    /** The list's entries, the pinned ones first; [canEdit] is false for another user's list. */
    fun apply(rates: List<Rate>, pinned: List<PinnedRate>, canEdit: Boolean): List<RateViewModel>
}
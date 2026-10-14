package com.gnoemes.shikimori.entity.common.presentation

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.RateStatus

/** A title card with a status button, as `BaseRateButtonListPresenter` needs it. */
interface RateButtonListItem {
    val id: Long
    /** `Constants.NO_ID` when the title is not in the user's list */
    val rateId: Long
    val type: Type
    val title: String
    val status: RateStatus?
}

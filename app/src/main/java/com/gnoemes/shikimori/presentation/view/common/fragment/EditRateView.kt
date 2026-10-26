package com.gnoemes.shikimori.presentation.view.common.fragment

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragmentView

interface EditRateView : BaseFragmentView {

    /** The entry once it is read - null for a title not in the list; until then the sheet shows placeholders. */
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showRate(rate: UserRate?)

    /** The entry could not be read: the error is a message, and the sheet closes. */
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun close()
}

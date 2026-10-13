package com.gnoemes.shikimori.presentation.view.base.fragment

import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.rates.domain.RateStatus

/** A screen listing titles with a status button on each: «Похожее», «Хронология». */
interface RateButtonListView : BaseFragmentView {

    fun showData(items: List<Any>)

    @StateStrategyType(SkipStrategy::class)
    fun showStatusDialog(id: Long, title: String, status: RateStatus?, anime: Boolean)
}

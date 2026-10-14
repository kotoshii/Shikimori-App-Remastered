package com.gnoemes.shikimori.presentation.view.chronology

import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.chronology.ChronologyType
import com.gnoemes.shikimori.presentation.view.base.fragment.RateButtonListView

interface ChronologyView : RateButtonListView {

    @StateStrategyType(SkipStrategy::class)
    fun scrollTo(pos: Int)

    @StateStrategyType(SkipStrategy::class)
    fun showTypeDialog(currentType: ChronologyType)
}

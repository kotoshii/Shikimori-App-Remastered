package com.gnoemes.shikimori.presentation.view.main

import com.arellomobile.mvp.viewstate.strategy.AddToEndStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.presentation.view.base.activity.BaseView

@StateStrategyType(AddToEndStrategy::class)
interface MainView : BaseView {

    /** Back to the first screen of the tab [screenKey]. */
    @StateStrategyType(SkipStrategy::class)
    fun clearBackStack(screenKey: String)

    /** The first screen's own action when it is showing and has one, else [clearBackStack]. */
    @StateStrategyType(SkipStrategy::class)
    fun tabRootActionOrClearBackStack(screenKey: String)

}
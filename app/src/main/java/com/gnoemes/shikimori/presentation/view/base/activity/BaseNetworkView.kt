package com.gnoemes.shikimori.presentation.view.base.activity

import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType

interface BaseNetworkView : BaseView {

    /**
     * A short pop-up: no connection, an HTTP error, "sign in first". Shown by the screen itself, so
     * it works on screens that have no router too (the player, the login, the bottom sheets).
     */
    @StateStrategyType(SkipStrategy::class)
    fun showErrorMessage(message: String)
}

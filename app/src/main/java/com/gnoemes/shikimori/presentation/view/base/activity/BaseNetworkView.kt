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

    /**
     * The comment a tap in user text points to, in a sheet over the screen (P8-T4, Q11); [shownTopicId] is
     * the topic the screen shows, if any. Screens that show user text have it (`BaseFragment`).
     */
    @StateStrategyType(SkipStrategy::class)
    fun showComment(id: Long, shownTopicId: Long?) = Unit
}

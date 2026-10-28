package com.gnoemes.shikimori.presentation.view.base.fragment

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.presentation.presenter.common.AddToEndSingleTagStrategy

//moxy can't work with generic views :(
interface BasePaginationView : BaseFragmentView {

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showData(data: List<Any>)

    fun showPageLoading()

    fun hidePageLoading()

    //«Повторить» at the end of the list for a page that failed; one tag, so only the last of the two is
    //shown again when the view is rebuilt
    @StateStrategyType(AddToEndSingleTagStrategy::class, tag = "pageError")
    fun showPageError()

    @StateStrategyType(AddToEndSingleTagStrategy::class, tag = "pageError")
    fun hidePageError()

    fun onAllData()

    /** Its owner's «Могут видеть мой список» hides the list from the viewer; [guest]: signed out. */
    fun showHiddenListView(guest: Boolean)

}
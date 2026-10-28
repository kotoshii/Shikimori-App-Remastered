package com.gnoemes.shikimori.presentation.view.clubs

import com.gnoemes.shikimori.entity.club.presentation.UserClubViewModel
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragmentView

interface UserClubsView : BaseFragmentView {

    fun showData(items: List<UserClubViewModel>)

    //the latest only: every call was replayed when the screen was rebuilt (BUGS #45)
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showClubsCount(count : Int)
}
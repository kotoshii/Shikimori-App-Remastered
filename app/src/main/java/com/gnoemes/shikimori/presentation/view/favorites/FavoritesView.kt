package com.gnoemes.shikimori.presentation.view.favorites

import com.gnoemes.shikimori.entity.user.presentation.FavoriteViewModel
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragmentView

interface FavoritesView : BaseFragmentView {

    fun showData(items : List<FavoriteViewModel>)

    //the latest only: every call was replayed when the screen was rebuilt (BUGS #45)
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showFavoritesCount(count : Int)
}
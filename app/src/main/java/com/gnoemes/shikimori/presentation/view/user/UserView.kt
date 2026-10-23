package com.gnoemes.shikimori.presentation.view.user

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.user.presentation.UserContentViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserHeadViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserInfoViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserRateViewModel
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragmentView

interface UserView : BaseFragmentView {

    fun setInfo(data: UserInfoViewModel)

    fun setHead(data: UserHeadViewModel)

    fun setFavorites(isMe: Boolean, it: UserContentViewModel)

    fun setFriends(isMe: Boolean, it: UserContentViewModel)

    fun setClubs(isMe: Boolean, it: UserContentViewModel)

    fun setAnimeRate(data: UserRateViewModel)

    fun setMangaRate(data: UserRateViewModel)

    fun addSettings()

    /** «Выйти» in the toolbar menu of the tab's own profile, only while signed in. */
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showSignOut(show: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun showSignOutDialog()

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showAuthView(show: Boolean)

    fun toggleAnimeRate(expanded: Boolean)
    fun toggleMangaRate(expanded: Boolean)
}
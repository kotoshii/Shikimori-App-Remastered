package com.gnoemes.shikimori.presentation.presenter.friends

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.user.presentation.FriendViewModel
import com.gnoemes.shikimori.presentation.presenter.base.BasePaginationPresenter
import com.gnoemes.shikimori.presentation.presenter.friends.converter.FriendsViewModelConverter
import com.gnoemes.shikimori.presentation.view.friends.FriendsView
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Single
import javax.inject.Inject

@InjectViewState
class FriendsPresenter @Inject constructor(
        private val interactor : UserInteractor,
        private val converter : FriendsViewModelConverter
) : BasePaginationPresenter<FriendViewModel, FriendsView>() {

    var id : Long = Constants.NO_ID

    //every friend, a page at a time: the api sends at most 100 a request and no total (BUGS #30)
    override fun getPaginatorRequestFactory(): (Int) -> Single<Page<FriendViewModel>> =
            { page -> interactor.getFriends(id, page).map { it.map(converter::apply) } }
}

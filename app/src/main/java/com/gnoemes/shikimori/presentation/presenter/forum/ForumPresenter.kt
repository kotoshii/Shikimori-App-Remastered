package com.gnoemes.shikimori.presentation.presenter.forum

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.topic.TopicInteractor
import com.gnoemes.shikimori.entity.common.domain.Screens
import com.gnoemes.shikimori.entity.forum.domain.Forum
import com.gnoemes.shikimori.entity.forum.domain.ForumType
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.view.forum.ForumView
import com.gnoemes.shikimori.presentation.presenter.forum.converter.ForumConverter
import com.gnoemes.shikimori.presentation.presenter.base.appendLoadingLogic
import javax.inject.Inject

@InjectViewState
class ForumPresenter @Inject constructor(
        private val interactor: TopicInteractor,
        private val converter: ForumConverter
) : BaseNetworkPresenter<ForumView>() {

    //one load at a time, closed with the screen: it was never disposed, and a refresh did not cancel
    //the load before it (T5-17)
    private val forumsLoad = loadSlot()

    override fun initData() {
        loadData()
    }

    private fun loadData(refresh: Boolean = false) = forumsLoad.start {
        interactor.getForums()
                .appendLoadingLogic(viewState, refresh)
                .map(converter)
                .subscribe(this::showData, loadErrors(overContent = refresh))
    }

    private fun showData(items: List<Forum>) {
        viewState.showData(items)
    }

    fun onForumClicked(type: ForumType) {
        router.navigateTo(Screens.TOPICS, type)
    }

    fun onRefresh() {
        loadData(refresh = true)
    }
}
package com.gnoemes.shikimori.presentation.presenter.series.episodes

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.series.SeriesInteractor
import com.gnoemes.shikimori.domain.series.WatchProgressInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.series.presentation.EpisodeViewModel
import com.gnoemes.shikimori.entity.series.presentation.EpisodesNavigationData
import com.gnoemes.shikimori.entity.user.domain.UserStatus
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.presenter.series.episodes.converter.EpisodeViewModelConverter
import com.gnoemes.shikimori.presentation.view.series.episodes.EpisodesView
import com.gnoemes.shikimori.utils.clearAndAddAll
import io.reactivex.Single
import javax.inject.Inject

@InjectViewState
class EpisodesPresenter @Inject constructor(
        private val interactor: SeriesInteractor,
        private val progressInteractor: WatchProgressInteractor,
        private val userInteractor: UserInteractor,
        private val converter: EpisodeViewModelConverter,
        private val resourceProvider: CommonResourceProvider
) : BaseNetworkPresenter<EpisodesView>() {

    lateinit var navigationData: EpisodesNavigationData
    private var isAlternativeSource = false

    private val items = mutableListOf<EpisodeViewModel>()
    private var query : String? = null

    override fun initData() {
        super.initData()
        isAlternativeSource = navigationData.isAlternative
        loadData()

        viewState.showAlternativeLabel(navigationData.isAlternative)
    }

    fun onRefresh() {
        loadData()
    }

    private fun loadData() =
            loadEpisodes()
                    .appendLoadingLogic(viewState)
                    .subscribe(this::setData, this::processErrors)
                    .addToDisposables()

    private fun loadEpisodes(): Single<List<EpisodeViewModel>> =
            interactor.getEpisodes(navigationData.animeId, isAlternativeSource)
                    .map{ converter.convert(it, navigationData.currentEpisode, userInteractor.getUserStatus())}

    private fun setData(items: List<EpisodeViewModel>) {
        val first = this.items.isEmpty()
        this.items.clearAndAddAll(items)
        showData(items)

        if (first) scrollToPenultimate()
    }

    private fun showData(items: List<EpisodeViewModel>) {
        if (!query.isNullOrBlank() && items.isEmpty()) viewState.showSearchEmpty()
        else if (items.isEmpty()) {
            viewState.showEmptyEpisodesView(true, isAlternativeSource)
            viewState.showContent(false)
        } else viewState.showData(items)
    }

    private fun scrollToPenultimate() {
        var lastWatchedWithIgnoredGaps = 0
        kotlin.run loop@{
            items.forEachIndexed { index, episodeViewModel ->
                if (episodeViewModel.isWatched) lastWatchedWithIgnoredGaps = index
                if (!episodeViewModel.isWatched && lastWatchedWithIgnoredGaps != 0) return@loop
            }
        }
        viewState.scrollToPosition(lastWatchedWithIgnoredGaps - 1)
    }

    fun onEpisodeClicked(item: EpisodeViewModel) {
        viewState.onEpisodeSelected(item.id, item.index, isAlternativeSource)
    }

    fun onEpisodeLongClick(item: EpisodeViewModel) {
        if (userInteractor.getUserStatus() == UserStatus.GUEST) {
            viewState.showSystemMessage(resourceProvider.needAuthRates)
            return
        }

        viewState.showEpisodeOptionsDialog(item.index)
    }

    fun onSearchClicked() {
        viewState.showSearchView()
    }

    fun onSearchClosed() {
        viewState.hideSearchView()
    }

    fun onAlternativeSourceClicked() {
        isAlternativeSource = !isAlternativeSource
        items.clear()
        viewState.showAlternativeLabel(isAlternativeSource)
        onRefresh()
    }

    fun onQueryChanged(newText: String?) {
       query = newText

        if (query.isNullOrBlank()) {
            showData(items)
        } else {
            val searchItems = items.filter { it.index.toString().contains(query ?: "") }
            showData(searchItems)
        }

        viewState.scrollToPosition(0)
    }

    //sets the count to [index] - the same as typing it in the edit drawer, raised only
    fun onCheckAllPrevious(index: Int) {
        progressInteractor.markWatchedUpTo(navigationData.animeId, index)
                .subscribe(this::showWatched, this::processErrors)
                .addToDisposables()
    }

    private fun showWatched() {
        val watched = progressInteractor.watchedEpisodes(navigationData.animeId)
        items.clearAndAddAll(items.map { it.copy(isWatched = it.index <= watched) })

        if (query.isNullOrBlank()) showData(items)
        else showData(items.filter { it.index.toString().contains(query ?: "") })
    }

    fun <T> Single<T>.appendLoadingLogic(viewState: EpisodesView): Single<T> =
            this.doOnSubscribe { viewState.onShowLoading() }
                    .doOnSubscribe { viewState.showEmptyEpisodesView(false) }
                    .doOnSubscribe { viewState.hideNetworkView() }
                    .doOnSubscribe { viewState.showContent(true) }
                    .doAfterTerminate { viewState.onHideLoading() }
                    .doOnEvent { _, _ -> viewState.onHideLoading() }
                    .doOnSuccess { viewState.showContent(true) }
}
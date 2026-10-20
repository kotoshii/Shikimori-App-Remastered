package com.gnoemes.shikimori.presentation.presenter.rates

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.local.preference.RateSortSource
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.domain.rates.PinnedRateInteractor
import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.auth.AuthType
import com.gnoemes.shikimori.entity.common.domain.Screens
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.DetailsAction
import com.gnoemes.shikimori.entity.common.presentation.RateSort
import com.gnoemes.shikimori.entity.common.presentation.SortAction
import com.gnoemes.shikimori.entity.main.BottomScreens
import com.gnoemes.shikimori.entity.rates.domain.*
import com.gnoemes.shikimori.entity.rates.presentation.RateCategory
import com.gnoemes.shikimori.entity.rates.presentation.RateSortViewModel
import com.gnoemes.shikimori.entity.rates.presentation.RateViewModel
import com.gnoemes.shikimori.entity.search.presentation.SearchNavigationData
import com.gnoemes.shikimori.entity.series.presentation.SeriesNavigationData
import com.gnoemes.shikimori.entity.user.domain.UserStatus
import com.gnoemes.shikimori.presentation.presenter.base.BasePaginationPresenter
import com.gnoemes.shikimori.presentation.presenter.common.paginator.ViewController
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.presenter.common.provider.SortResourceProvider
import com.gnoemes.shikimori.presentation.presenter.rates.converter.RateCountConverter
import com.gnoemes.shikimori.presentation.presenter.rates.converter.RateViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.rates.provider.RateResourceProvider
import com.gnoemes.shikimori.presentation.view.rates.RateView
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@InjectViewState
class RatePresenter @Inject constructor(
        private val userInteractor: UserInteractor,
        private val ratesInteractor: RatesInteractor,
        private val sortResourceProvider: SortResourceProvider,
        private val pinInteractor: PinnedRateInteractor,
        private val resourceProvider: CommonResourceProvider,
        private val settingsSource: SettingsSource,
        private val sortSource: RateSortSource,
        private val rateCountConverter: RateCountConverter,
        private val rateResourceProvider: RateResourceProvider,
        private val converter: RateViewModelConverter
) : BasePaginationPresenter<Rate, RateView>(), ViewController<Rate> {

    var type: Type = Type.ANIME
    /** The user these lists were opened for, from a profile; null for the «Списки» tab, the signed-in user's own. */
    var ownerId: Long? = null
    var rateStatus: RateStatus? = null
    var priorityStatus: RateStatus? = null

    //whose lists are shown now. The signed-in user's are looked up on every load, never kept: the app
    //may have signed out, or another account signed in, since (BUGS #70)
    private var userId: Long = Constants.NO_ID

    private val isAnime: Boolean
        get() = type == Type.ANIME

    private val isUserExist: Boolean
        get() = userId != Constants.NO_ID

    private val isSignedOutOfMyList: Boolean
        get() = ownerId == null && userInteractor.getUserStatus() == UserStatus.GUEST


    private var isDescendingSort: Boolean = false
    private var sort: RateSort = RateSort.Id
    private var query: String? = null
    private var pinnedRates: Int = 0

    //the loaded entries of this tab, without the ones waiting to be changed or deleted
    private var rates: List<Rate> = emptyList()

    private val pendingChanges = PendingRateChanges()

    //the sort and search in progress: a newer one replaces it, so an older result never shows last
    private val shownRates = SerialDisposable()

    init {
        shownRates.addToDisposables()
    }

    override fun onViewReattached() {
        loadUserOrCategories()
        if (rateStatus != null) onRefresh()
    }

    override fun initData() {
        viewState.setNavigationItems(emptyList())

        sort = sortSource.getSort(type)
        isDescendingSort = sortSource.getOrder(type)

        viewState.selectType(type)
        loadUserOrCategories()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Rate categories logic
    ///////////////////////////////////////////////////////////////////////////

    private fun loadUserOrCategories() {
        val owner = ownerId
        when {
            owner != null -> {
                userId = owner
                loadRateCategories()
            }
            isSignedOutOfMyList -> showSignedOut()
            else -> loadMyUser()
        }
    }

    private fun loadRateCategories() {
        if (isSignedOutOfMyList) {
            showSignedOut()
            return
        }
        userInteractor.getDetails(userId)
                .map {
                    if (isAnime) rateCountConverter.countAnimeRates(it)
                    else rateCountConverter.countMangaRates(it)
                }
                .subscribe(this::setRateData, this::processErrors)
                .addToDisposables()
    }

    //another account than the one shown starts from its own first tab
    private fun loadMyUser() =
            userInteractor.getMyUserId()
                    .subscribe({
                        if (it != userId) forgetShownLists()
                        userId = it
                        loadRateCategories()
                    }, this::processErrors)
                    .addToDisposables()

    //nothing of the lists shown before stays: the user signed out, or another account signed in
    private fun forgetShownLists() {
        destroyPaginator()
        rateStatus = null
        rates = emptyList()
    }

    //the «Войти» view of the «Списки» tab
    private fun showSignedOut() {
        forgetShownLists()
        userId = Constants.NO_ID
        viewState.onHideLoading()
        viewState.setNavigationItems(emptyList())
        viewState.hideNetworkView()
        viewState.showEmptyRatesView(false)
        viewState.showContent(false)
        viewState.showNeedAuthView(true)
    }

    private fun setRateData(items: List<RateCategory>) {
        //signed out while loading: a request that found no readable login went on as a guest (#21)
        if (isSignedOutOfMyList) {
            showSignedOut()
            return
        }
        viewState.setNavigationItems(items)
        viewState.showNeedAuthView(false)
        if (items.isNotEmpty()) {
            if (rateStatus == null) onChangeStatus(priorityStatus ?: items.first().status)
            viewState.showEmptyRatesView(false)
            viewState.hideNetworkView()
            viewState.showContent(true)
            priorityStatus = null
        } else {
            viewState.showEmptyRatesView(true, isAnime)
            viewState.hideNetworkView()
            viewState.showContent(false)
        }
    }

    //the tab's own lists show «Войти» after a sign-out, whatever failed
    override fun processErrors(throwable: Throwable) {
        if (isSignedOutOfMyList) showSignedOut()
        else super.processErrors(throwable)
    }

    fun onChangeType() {
        if (isUserExist) {
            destroyPaginator()
            rateStatus = null
            this.type = if (type == Type.ANIME) Type.MANGA else Type.ANIME
            viewState.selectType(type)
            loadRateCategories()
        }
    }

    fun onChangeStatus(rateStatus: RateStatus) {
        if (isUserExist) {
            this.rateStatus = rateStatus
            viewState.scrollToTop()
            viewState.selectRateStatus(rateStatus)
            loadData()
        }
    }

    fun onEmptyRateClicked(anime: Boolean) {
        router.navigateTo(BottomScreens.SEARCH, SearchNavigationData(null, if (anime) Type.ANIME else Type.MANGA))
    }

    fun onChangeCanceled(rateId: Long) {
        pendingChanges.cancel(rateId)
        onRefresh()
    }

    override fun onDestroy() {
        pendingChanges.sendAll()
        super.onDestroy()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Rate list logic
    ///////////////////////////////////////////////////////////////////////////

    override fun loadData() {
        if (isSignedOutOfMyList) showSignedOut()
        else super.loadData()
    }

    override fun getPaginatorRequestFactory(): (Int) -> Single<List<Rate>> {
        return if (rateStatus == null) { page: Int -> Single.error(IllegalStateException()) }
        else if (isAnime) { page: Int -> ratesInteractor.getAnimeRates(userId, page, Constants.MAX_LIMIT, rateStatus!!) }
        else { page: Int -> ratesInteractor.getMangaRates(userId, page, Constants.MAX_LIMIT, rateStatus!!) }
    }

    override fun showEmptyError(show: Boolean, throwable: Throwable?) {
        if (show) processErrors(throwable!!)
        else viewState.apply {
            hideEmptyView()
            hideNetworkView()
        }
    }

    override fun showData(show: Boolean, data: List<Rate>) {
        if (isSignedOutOfMyList) {
            showSignedOut()
            return
        }
        rates = data.filterNot { pendingChanges.isWaiting(it.id) }
        if (show) showRates()
        else viewState.showContent(show)
    }

    override fun showEmptyView(show: Boolean) {
        if (show) {
            rateStatus = null
            loadUserOrCategories()
        }
    }

    fun onQueryChanged(newText: String?) {
        query = newText
        showRates()
    }

    fun onAction(it: DetailsAction) {
        when (it) {
            is DetailsAction.ChangeRateStatus -> onChangeRateStatus(it.id, it.newStatus)
            is DetailsAction.WatchOnline -> onWatchOnline(it.id!!)
        }
    }

    fun onListAction(it: RateListAction) {
        when (it) {
            is RateListAction.Pin -> onPinRate(it.rate)
            is RateListAction.ChangeOrder -> onPinOrderChanged(it.rate, it.newOrder)
            is RateListAction.SwipeAction -> onRateSwipeAction(it.rate, it.action)
            is RateListAction.EditRate -> onEditRate(it.rate)
        }
    }

    private fun onRateSwipeAction(rate: RateViewModel, action: RateSwipeAction) {
        val rateItem = rates.find { it.id == rate.id }
        if (rateItem != null) {
            when (action) {
                RateSwipeAction.INCREMENT -> onIncrementRate(rateItem)
                RateSwipeAction.ON_HOLD -> onChangeRateStatus(rateItem.id, RateStatus.ON_HOLD)
                RateSwipeAction.DROP -> onChangeRateStatus(rateItem.id, RateStatus.DROPPED)
                RateSwipeAction.CHANGE -> onShowStatusDialog(rate)
                else -> Unit
            }
        }
    }

    private fun onShowStatusDialog(rate: RateViewModel) {
        viewState.showStatusDialog(rate.id, rate.name, rate.rawRate.status, rate.type == Type.ANIME)
        showRates()
    }

    private fun onIncrementRate(rate: Rate) {
        ratesInteractor.increment(rate.id)
                .subscribe({ onRefresh() }, this::processErrors)
                .addToDisposables()
    }

    private fun onPinOrderChanged(rate: RateViewModel, newOrder: Int) {
        pinInteractor.addPinnedRate(PinnedRate(rate.contentId, rate.type, rateStatus!!, newOrder))
                .subscribe({}, this::processErrors)
                .addToDisposables()
    }

    private fun onPinRate(rate: RateViewModel) {
        if (rate.isPinned) pinInteractor.removePinnedRate(rate.contentId)
                .subscribe(this::showRates, this::processErrors)
                .addToDisposables()
        else {
            if (pinnedRates >= Constants.MAX_PINNED_RATES) viewState.showPinLimitMessage()
            else pinInteractor.addPinnedRate(PinnedRate(rate.contentId, type, rateStatus!!, if (pinnedRates == 0) 0 else pinnedRates - 1))
                    .subscribe(this::showRates, this::processErrors)
                    .addToDisposables()
        }
    }

    fun onSortAction(it: SortAction) {
        when (it) {
            is SortAction.ChangeOrder -> onSortChanged(sort, it.isDescending)
            is SortAction.ChangeSort -> onShowSortDialog(it.sorts)
        }
    }

    private fun onShowSortDialog(sorts: List<Triple<RateSort, String, Boolean>>) {
        viewState.showSortDialog(sorts)
    }

    fun onOpenRandom() {
        val item = rates.shuffled().firstOrNull()
        if (item != null) {
            if (item.anime != null) onAnimeClicked(item.anime.id)
            else if (item.manga != null) onMangaClicked(item.manga.id)
        } else router.showSystemMessage(resourceProvider.emptyMessage)
    }

    private fun onEditRate(rate: RateViewModel) {
        val userRate = getUserRate(rate.rawRate)
        viewState.showRateDialog(rate.name, userRate)
    }

    private fun getUserRate(rate: Rate?) = UserRate(
            id = rate?.id,
            targetId = rate?.anime?.id ?: rate?.manga?.id,
            targetType = type,
            score = rate?.score?.toDouble(),
            status = rateStatus,
            userId = userId,
            episodes = rate?.episodes,
            chapters = rate?.chapters,
            text = rate?.text
    )

    //the list can be older than the count - it may have changed on the site since - so the entry is
    //read again. The series screen opens on the episode after the last watched one
    private fun onWatchOnline(rateId: Long) {
        val rate = rates.find { it.id == rateId } ?: return
        val anime = rate.anime ?: return
        val name = settingsSource.titleName(anime.name, anime.nameRu)

        ratesInteractor.getRate(rate.id)
                .map { it.episodes ?: 0 }
                .subscribe({ watched ->
                    val data = SeriesNavigationData.of(anime.id, anime.image, name, anime.status, anime.episodes,
                            anime.episodesAired, rate.id, watched, watched + 1)
                    router.navigateTo(Screens.SERIES, data)
                }, this::processErrors)
                .addToDisposables()
    }

    fun onChangeRateStatus(id: Long, newStatus: RateStatus) {
        val item = rates.find { it.id == id } ?: return
        if (item.status == newStatus) {
            showRates()
            return
        }
        waitAndSend(item, ratesInteractor.changeRateStatus(id, newStatus),
                rateResourceProvider.getChangeRateStatusMessage(item.type, newStatus))
    }

    fun onDeleteRate(id: Long) {
        val item = rates.find { it.id == id } ?: return
        waitAndSend(item, ratesInteractor.deleteRate(id), rateResourceProvider.getDeleteRateMessage(item.type))
    }

    //the entry leaves the list at once; the change is sent after the «Отменить» wait. A change that
    //fails brings the entry back
    private fun waitAndSend(item: Rate, change: Completable, message: String) {
        pendingChanges.add(item.id, change, this::onRateChanged) { onRefresh(); processErrors(it) }
        viewState.showRateMessage(message, item.id)
        rates = rates - item
        showRates()
    }

    fun onUpdateRate(rate: UserRate) =
            ratesInteractor.updateRate(rate)
                    .subscribe(this::onRateChanged, this::processErrors)
                    .addToDisposables()

    //the tab counts change too
    private fun onRateChanged() {
        loadUserOrCategories()
        onRefresh()
    }

    fun onSortChanged(sort: RateSort) {
        onSortChanged(sort, isDescendingSort)
    }

    private fun onSortChanged(sort: RateSort, desc: Boolean) {
        isDescendingSort = desc
        this.sort = sort
        sortSource.saveSort(type, sort)
        sortSource.saveOrder(type, desc)
        showRates()
    }

    /** Sorts and searches the entries in the background, then shows them under the sort bar. */
    private fun showRates() {
        val status = rateStatus ?: return
        //read here, on the main thread; the work below runs on another
        val entries = rates
        val currentSort = sort
        val descending = isDescendingSort
        val search = query?.takeIf { it.isNotBlank() }
        val sorts = if (isAnime) sortResourceProvider.getAnimeRateSorts() else sortResourceProvider.getMangaRateSorts()
        val sortBar = RateSortViewModel(status, currentSort, sorts, descending, isAnime)

        shownRates.set(pinInteractor.getPinnedRates(type, status)
                .observeOn(Schedulers.computation())
                .map { pinned ->
                    val shown = sortAndSearchRates(entries, currentSort, descending, search) { name, nameRu -> settingsSource.titleName(name, nameRu) }
                    converter.apply(shown, pinned)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ setData(sortBar, it, search != null) }, this::processErrors))
    }

    private fun setData(sortBar: RateSortViewModel, items: List<RateViewModel>, isSearch: Boolean) {
        if (!isSearch) pinnedRates = items.count { it.isPinned }
        if (isSearch && items.isEmpty()) viewState.showEmptySearchView(listOf(sortBar))
        else viewState.showData(listOf(sortBar) + items)
    }

    fun onSignIn() = openAuth(AuthType.SIGN_IN)
    fun onSignUp() = openAuth(AuthType.SIGN_UP)

    private fun openAuth(type: AuthType) {
        router.navigateTo(Screens.AUTHORIZATION, type)
    }
}




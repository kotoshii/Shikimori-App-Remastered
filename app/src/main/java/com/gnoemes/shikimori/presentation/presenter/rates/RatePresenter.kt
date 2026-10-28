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
import com.gnoemes.shikimori.entity.common.presentation.ListDividerItem
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
import com.gnoemes.shikimori.presentation.presenter.base.PendingRateChanges
import com.gnoemes.shikimori.presentation.presenter.common.paginator.ViewController
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.presenter.common.provider.SortResourceProvider
import com.gnoemes.shikimori.presentation.presenter.rates.converter.RateCountConverter
import com.gnoemes.shikimori.presentation.presenter.rates.converter.RateViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.rates.provider.RateResourceProvider
import com.gnoemes.shikimori.presentation.view.rates.RateView
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
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
    /** The owner as the profile had them, shown at once; the lists' own read of the user follows. */
    var ownerNickname: String? = null
    var ownerAvatar: String? = null
    var rateStatus: RateStatus? = null
    var priorityStatus: RateStatus? = null

    //whose lists are shown now. The signed-in user's are looked up on every load, never kept: the app
    //may have signed out, or another account signed in, since (BUGS #70)
    private var userId: Long = Constants.NO_ID

    private val isAnime: Boolean
        get() = type == Type.ANIME

    private val isUserExist: Boolean
        get() = userId != Constants.NO_ID

    //whether the lists shown are the signed-in user's own - from a profile they can be someone else's,
    //which shikimori lets no one change (BUGS #85). Set with the lists' counts, before any entry shows
    private var isMyList = false

    private val isSignedOutOfMyList: Boolean
        get() = ownerId == null && userInteractor.getUserStatus() == UserStatus.GUEST


    //the sort saved for the type shown, read each time: a copy kept here missed the anime ↔ manga switch (BUGS #7)
    private val sort: RateSort
        get() = sortSource.getSort(type)

    private val isDescendingSort: Boolean
        get() = sortSource.getOrder(type)

    private var query: String? = null
    private var pinnedRates: Int = 0

    //the loaded entries of this tab, without the ones waiting to be changed or deleted
    private var rates: List<Rate> = emptyList()

    private val pendingChanges = PendingRateChanges()

    //the sort and search in progress: a newer one replaces it, so an older result never shows last
    private val shownRates = loadSlot()

    override fun onViewReattached() {
        loadUserOrCategories()
        if (rateStatus != null) onRefresh()
    }

    override fun initData() {
        viewState.setNavigationItems(emptyList())
        viewState.selectType(type)
        //with the bar: it waited for the read of the user, a moment after the screen opened (P7-T5 Test 91)
        ownerNickname?.let { viewState.showListOwner(it, ownerAvatar) }
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
                .doOnSuccess {
                    isMyList = it.isMe
                    viewState.showListOwner(if (it.isMe) null else it.nickname, it.image.x48)
                }
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
        //a sort still running would show them again: rebuilding the tab's view restarts one
        shownRates.cancel()
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
            //a pull on the empty list: no list load follows to hide the spinner
            viewState.onHideLoading()
        }
    }

    //the tab's own lists show «Войти» after a sign-out, whatever failed
    override fun processErrors(throwable: Throwable) {
        if (isSignedOutOfMyList) {
            //once, while the lists are still shown: several requests can fail on one sign-out
            if (userId != Constants.NO_ID) showSignedOutMessage(throwable)
            showSignedOut()
        } else super.processErrors(throwable)
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

    //nothing to load before a status is chosen: the categories' load picks one, then loads it. A
    //paginator made earlier kept its request for no status - an error each time - and the list stayed
    //blank: a change sent from a sheet restored after Android closed the app refreshed before the
    //categories came (P7-T5 Test 87i)
    override fun loadData() {
        if (isSignedOutOfMyList) showSignedOut()
        else if (rateStatus != null) super.loadData()
    }

    //with no status chosen - an empty list, or one still loading - a pull asks for the categories
    //again: a list filled on the site since then shows up
    override fun onRefresh() {
        if (rateStatus == null) loadUserOrCategories()
        else super.onRefresh()
    }

    override fun getPaginatorRequestFactory(): (Int) -> Single<Page<Rate>> {
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

    //signed out while a later page loaded or the list refreshed: «Войти», as for every other request of the tab
    override fun takesScreen(throwable: Throwable) = isSignedOutOfMyList || super.takesScreen(throwable)

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
                .subscribe({ showRates(); viewState.showPinChangedMessage(false) }, this::processErrors)
                .addToDisposables()
        else {
            if (pinnedRates >= Constants.MAX_PINNED_RATES) viewState.showPinLimitMessage()
            else pinInteractor.addPinnedRate(PinnedRate(rate.contentId, type, rateStatus!!, if (pinnedRates == 0) 0 else pinnedRates - 1))
                    .subscribe({ showRates(); viewState.showPinChangedMessage(true) }, this::processErrors)
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

    //the sheet reads the entry itself, as shikimori has it now: the list can be older, and a copy built
    //from its row lost the rewatch count, which ✓ then saved as 0 (BUGS #82, #83)
    private fun onEditRate(rate: RateViewModel) {
        viewState.showRateDialog(rate.name, rate.contentId, rate.type)
    }

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
                            anime.episodesAired, rate.id, watched)
                    router.navigateTo(Screens.SERIES, data)
                }, this::processErrors)
                .addToDisposables()
    }

    fun onChangeRateStatus(id: Long, newStatus: RateStatus) {
        val item = rates.find { it.id == id } ?: return sendNow(ratesInteractor.changeRateStatus(id, newStatus))
        if (item.status == newStatus) {
            showRates()
            return
        }
        waitAndSend(item, ratesInteractor.changeRateStatus(id, newStatus),
                rateResourceProvider.getChangeRateStatusMessage(item.type, newStatus))
    }

    fun onDeleteRate(id: Long) {
        val item = rates.find { it.id == id } ?: return sendNow(ratesInteractor.deleteRate(id))
        waitAndSend(item, ratesInteractor.deleteRate(id), rateResourceProvider.getDeleteRateMessage(item.type))
    }

    //the sheet and the status dialog can outlive the list: when Android closed the app in the
    //background, they come back before the list has loaded, and their change used to do nothing. With
    //no row to hide and bring back, it is sent at once (BUGS #85)
    private fun sendNow(change: Completable) {
        change.subscribe(this::onRateChanged, this::showErrorText)
                .addToDisposables()
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
        //with no status chosen yet, the categories' load above picks one and loads it
        if (rateStatus != null) onRefresh()
    }

    fun onSortChanged(sort: RateSort) {
        onSortChanged(sort, isDescendingSort)
    }

    private fun onSortChanged(sort: RateSort, desc: Boolean) {
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
        val canEdit = isMyList
        val sorts = if (isAnime) sortResourceProvider.getAnimeRateSorts() else sortResourceProvider.getMangaRateSorts()
        val sortBar = RateSortViewModel(status, currentSort, sorts, descending, isAnime)

        //the pins are the user's own, kept by title: on another user's list they would reorder it by
        //the viewer's pins (P7-T5 Q146)
        val pins = if (canEdit) pinInteractor.getPinnedRates(type, status) else Single.just(emptyList<PinnedRate>())

        shownRates.start {
            pins
                    .observeOn(Schedulers.computation())
                    .map { pinned ->
                        val shown = sortAndSearchRates(entries, currentSort, descending, search) { name, nameRu -> settingsSource.titleName(name, nameRu) }
                        converter.apply(shown, pinned, canEdit)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ setData(sortBar, it, search != null) }, this::processErrors)
        }
    }

    private fun setData(sortBar: RateSortViewModel, items: List<RateViewModel>, isSearch: Boolean) {
        if (!isSearch) pinnedRates = items.count { it.isPinned }
        if (isSearch && items.isEmpty()) viewState.showEmptySearchView(listOf(sortBar))
        else viewState.showData(listOf(sortBar) + withPinnedGroup(items))
    }

    //the pinned entries come first, with a line under them: with the pin on each, it shows why they
    //are on top (BUGS #49)
    private fun withPinnedGroup(items: List<RateViewModel>): List<Any> {
        val (pinned, others) = items.partition { it.isPinned }
        return if (pinned.isEmpty() || others.isEmpty()) items
        else pinned + ListDividerItem + others
    }

    fun onSignIn() = openAuth(AuthType.SIGN_IN)
    fun onSignUp() = openAuth(AuthType.SIGN_UP)

    private fun openAuth(type: AuthType) {
        router.navigateTo(Screens.AUTHORIZATION, type)
    }
}




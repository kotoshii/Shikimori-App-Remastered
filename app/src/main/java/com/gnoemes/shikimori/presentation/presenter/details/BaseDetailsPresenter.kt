package com.gnoemes.shikimori.presentation.presenter.details

import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.chronology.ChronologyNavigationData
import com.gnoemes.shikimori.entity.common.domain.*
import com.gnoemes.shikimori.entity.common.presentation.DetailsAction
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentType
import com.gnoemes.shikimori.entity.common.presentation.DetailsHeadItem
import com.gnoemes.shikimori.entity.common.presentation.PlaceholderItem
import com.gnoemes.shikimori.entity.main.BottomScreens
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.entity.roles.domain.Character
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.entity.search.presentation.SearchNavigationData
import com.gnoemes.shikimori.entity.search.presentation.SearchPayload
import com.gnoemes.shikimori.entity.user.domain.UserStatus
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.presenter.common.converter.DetailsContentViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.view.details.BaseDetailsView
import com.gnoemes.shikimori.presentation.presenter.base.appendLightLoadingLogic
import com.gnoemes.shikimori.presentation.presenter.base.appendLoadingLogic
import com.gnoemes.shikimori.utils.applySingleSchedulers
import com.gnoemes.shikimori.utils.clearAndAddAll
import com.gnoemes.shikimori.utils.firstUpperCase
import io.reactivex.Completable
import io.reactivex.Single

/**
 * The anime and the manga page. Everything that reads the title's details waits until they have
 * loaded ([details] is null before that), so a menu tapped early does nothing instead of crashing.
 */
abstract class BaseDetailsPresenter<View : BaseDetailsView, D : TitleDetails>(
        protected val ratesInteractor: RatesInteractor,
        protected val userInteractor: UserInteractor,
        protected val resourceProvider: CommonResourceProvider,
        protected val contentConverter: DetailsContentViewModelConverter,
        private val settingsSource: SettingsSource
) : BaseNetworkPresenter<View>() {

    var id: Long = Constants.NO_ID

    protected var rateId: Long = Constants.NO_ID

    /** Read when needed, never kept: the app may sign the user out while the page is open (BUGS #70). */
    private val isGuest: Boolean
        get() = userInteractor.getUserStatus() == UserStatus.GUEST

    /** The title's details, once loaded. */
    protected var details: D? = null

    //✏'s read of the entry: a second tap replaces the first, so one sheet opens
    private val rateSheetLoad = loadSlot()

    protected val characters = mutableListOf<Character>()
    protected val creators = mutableListOf<Pair<Person, List<String>>>()

    abstract val type: Type

    protected abstract val detailsConverter: TitleDetailsViewModelConverter<D>

    protected abstract fun fetchDetails(): Single<D>

    abstract val characterFactory: (id: Long) -> Single<Roles>

    abstract val relatedFactory: (id: Long) -> Single<List<Related>>

    abstract val linkFactory: (id: Long) -> Single<List<Link>>

    /** The title's name, by the naming setting; empty before the details have loaded. */
    protected val title: String
        get() = details?.let { settingsSource.titleName(it.name, it.nameRu) }.orEmpty()

    private inline fun withDetails(action: (D) -> Unit) {
        details?.let(action)
    }

    override fun initData() {
        loadData()
    }

    override fun onViewReattached() {
        redrawHead()
    }

    //the head again: its rate buttons follow the sign-in as it is now
    private fun redrawHead() {
        loadDetails()
                .subscribe({ viewState.setHeadItem(it) }, this::processErrors)
                .addToDisposables()
    }

    private fun loadDetails(): Single<DetailsHeadItem> =
            fetchDetails()
                    .doOnSuccess { details = it; rateId = it.userRate?.id ?: Constants.NO_ID }
                    .map { detailsConverter.convertHead(it, isGuest) }

    private fun loadContent(showLoading: Boolean = true): Single<DetailsHeadItem> =
            loadDetails()
                    .flatMap {
                        if (showLoading) Single.just(it).appendLoadingLogic(viewState)
                        else Single.just(it)
                    }
                    .doOnSuccess { withDetails { onDetailsLoaded(it) } }

    /** The page's sections, once the details are in; the anime page adds its videos and screenshots. */
    protected open fun onDetailsLoaded(details: D) {
        viewState.setInfoItem(detailsConverter.convertInfo(details, creators))
        viewState.setActionItem(detailsConverter.getActions(details.status))
        viewState.setDescriptionItem(detailsConverter.convertDescriptionItem(details.description))
    }

    protected open fun loadData() =
            loadContent()
                    .doOnSuccess { loadCharacters() }
                    .doOnSuccess { loadRelated() }
                    .subscribe({ viewState.setHeadItem(it) }, this::processErrors)
                    .addToDisposables()

    protected open fun loadCharacters() =
            characterFactory.invoke(id)
                    .doOnSuccess { setCreators(it.persons) }
                    .map { it.characters }
                    .doOnSuccess { characters.clearAndAddAll(it) }
                    .map(contentConverter)
                    .subscribe({ viewState.setContentItem(DetailsContentType.CHARACTERS, it) }, this::processErrors)
                    .addToDisposables()

    protected open fun loadRelated() =
            relatedFactory.invoke(id)
                    .map(contentConverter)
                    .subscribe({ viewState.setContentItem(DetailsContentType.RELATED, it) }, this::processErrors)
                    .addToDisposables()

    protected open fun loadLinks() =
            linkFactory.invoke(id)
                    .appendLightLoadingLogic(viewState)
                    .map { links -> links.map { it.copy(name = it.name!!.replace("_", " ").firstUpperCase()!!) } }
                    .subscribe({
                        if (it.isNotEmpty()) viewState.showLinks(it)
                        else router.showSystemMessage(resourceProvider.emptyMessage)
                    }, this::processErrors)
                    .addToDisposables()

    //the creators are rows of the info block, so it is drawn again with them
    private fun setCreators(creators: List<Pair<Person, List<String>>>) {
        this.creators.clearAndAddAll(creators)
        withDetails { viewState.setInfoItem(detailsConverter.convertInfo(it, this.creators)) }
    }

    protected open fun createRate(rate: UserRate?, newStatus: RateStatus? = null) = whenSignedIn {
        ratesInteractor.createRate(id, type, rate ?: UserRate(status = newStatus))
                .updateContentData()
    }

    protected open fun updateRate(rate: UserRate) = whenSignedIn {
        ratesInteractor.updateRate(rate)
                .updateContentData()
    }

    protected open fun changeRateStatus(newStatus: RateStatus) = whenSignedIn {
        ratesInteractor.changeRateStatus(rateId, newStatus)
                .updateContentData()
    }

    //a page drawn before an automatic sign-out still shows its rate buttons: tell the user, and draw
    //the head again without them
    private inline fun whenSignedIn(action: () -> Unit) {
        if (!isGuest) action()
        else {
            router.showSystemMessage(resourceProvider.needAuth)
            redrawHead()
        }
    }

    open fun onUpdateOrCreateRate(rate: UserRate) {
        when (rate.id) {
            Constants.NO_ID -> createRate(rate)
            else -> updateRate(rate)
        }
    }

    open fun onDeleteRate(id: Long) = whenSignedIn {
        ratesInteractor.deleteRate(id)
                .updateContentData()
    }

    open fun onAction(action: DetailsAction) {
        when (action) {
            is DetailsAction.Links -> onLinks()
            is DetailsAction.Link -> onLink(action.url, action.share)
            is DetailsAction.Chronology -> onChronology()
            is DetailsAction.WatchOnline -> onWatchOnline()
            is DetailsAction.EditRate -> onEditRate()
            is DetailsAction.OpenInBrowser -> onOpenInBrowser()
            is DetailsAction.Video -> onOpenWeb(action.url)
            is DetailsAction.GenreClicked -> onGenreClicked(action.genre)
            is DetailsAction.ChangeRateStatus -> onChangeRateStatus(action.newStatus)
            is DetailsAction.Screenshots -> onScreenshotsClicked(action.pos)
            is DetailsAction.Discussion -> onOpenDiscussion()
            is DetailsAction.StudioClicked -> onStudioClicked(action.id, action.name)
            is DetailsAction.RateStatusDialog -> onStatusDialog()
            is DetailsAction.Similar -> onSimilarClicked()
            is DetailsAction.Statistic -> onStatisticClicked()
            is DetailsAction.Share -> onShareClicked()
        }
    }

    private fun onLink(url: String, share: Boolean) {
        val screen = if (share) Screens.SHARE else Screens.WEB
        router.navigateTo(screen, url)
    }

    private fun onShareClicked() = withDetails {
        router.navigateTo(Screens.SHARE, it.url)
    }

    private fun onStatisticClicked() = withDetails { details ->
        Single.fromCallable { Pair(detailsConverter.convertScores(details.rateScoresStats), detailsConverter.convertStatuses(details.rateStatusesStats)) }
                .applySingleSchedulers()
                .subscribe({ viewState.showStatistic(title, it.first, it.second) }, this::processErrors)
                .addToDisposables()
    }

    private fun onSimilarClicked() {
        router.navigateTo(Screens.SIMILAR, CommonNavigationData(id, type))
    }

    //with the title's own address, for the franchise page's web link
    private fun onChronology() = withDetails {
        router.navigateTo(Screens.CHRONOLOGY, ChronologyNavigationData(id, type, it.url))
    }

    private fun onLinks() {
        loadLinks()
    }

    //the name goes along, so the filter sheet can show which studio the list is limited to (BUGS #13)
    private fun onStudioClicked(id: Long, name: String) {
        router.navigateTo(BottomScreens.SEARCH, SearchNavigationData(SearchPayload(studioId = id, studioName = name), Type.ANIME))
    }

    open fun onChangeRateStatus(newStatus: RateStatus) {
        when (rateId) {
            Constants.NO_ID -> createRate(null, newStatus)
            else -> changeRateStatus(newStatus)
        }
    }

    open fun onCharacterSearch(newText: String?) {
        if (newText.isNullOrBlank()) viewState.setContentItem(DetailsContentType.CHARACTERS, contentConverter.apply(characters))
        else {
            val searchItems: MutableList<Any> = characters.filter { it.name.contains(newText, true) || it.nameRu?.contains(newText, true) ?: false }.toMutableList()
            if (searchItems.isEmpty()) searchItems.add(PlaceholderItem())
            viewState.setContentItem(DetailsContentType.CHARACTERS, contentConverter.apply(searchItems))
        }
    }

    private fun onGenreClicked(genre: GenreV2) {
        router.navigateTo(BottomScreens.SEARCH, SearchNavigationData(SearchPayload(genre), type))
    }

    private fun onOpenDiscussion() = withDetails { details ->
        details.topicId?.let { onTopicClicked(it) }
                ?: router.showSystemMessage(resourceProvider.topicNotFound)
    }

    private fun onOpenInBrowser() = withDetails {
        onOpenWeb(it.url)
    }

    //the title is read again first, so the sheet gets the entry as shikimori has it now - the page can
    //be older: an entry added since (by «Автоматическое добавление», on the site) was then overwritten
    //with the empty sheet's zeros (BUGS #83, #84). The head is drawn again with it
    private fun onEditRate() {
        if (details == null) return
        rateSheetLoad.start {
            loadDetails()
                    .appendLightLoadingLogic(viewState)
                    .subscribe({ head ->
                        viewState.setHeadItem(head)
                        withDetails { viewState.showRateDialog(title, it.userRate, type == Type.ANIME) }
                    }, this::showErrorText)
        }
    }

    private fun onStatusDialog() = withDetails {
        viewState.showStatusDialog(id, title, it.userRate?.status, type == Type.ANIME)
    }

    /** Only the anime page has screenshots. */
    protected open fun onScreenshotsClicked(pos: Int) {
    }

    /** Only the anime page can be watched. */
    protected open fun onWatchOnline() {
    }

    protected open fun Completable.updateContentData() {
        andThen(loadContent(false))
                .doOnSuccess { viewState.setHeadItem(it) }
                .subscribe({ }, this@BaseDetailsPresenter::processErrors)
                .addToDisposables()
    }
}

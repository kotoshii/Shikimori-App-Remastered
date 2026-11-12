package com.gnoemes.shikimori.presentation.presenter.search

import android.util.Log
import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.domain.search.SearchInteractor
import com.gnoemes.shikimori.domain.search.filter.FilterInteractor
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import com.gnoemes.shikimori.entity.common.domain.SearchConstants
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.search.domain.CatalogSettings
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.entity.search.presentation.SearchPayload
import com.gnoemes.shikimori.presentation.presenter.base.BasePaginationPresenter
import com.gnoemes.shikimori.presentation.presenter.search.converter.SearchViewModelConverter
import com.gnoemes.shikimori.presentation.view.search.SearchView
import com.gnoemes.shikimori.presentation.presenter.base.appendLoadingLogic
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit
import javax.inject.Inject

//TODO studios
/**
 * The catalog. Its list holds the titles (or characters, people) as loaded, and draws them as cards
 * with the «Отображение» settings and the user's list statuses (P10-T3), so a changed setting or a
 * status read later draws the cards on screen again, with no request.
 */
@InjectViewState
class SearchPresenter @Inject constructor(
        private val interactor: SearchInteractor,
        private val converter: SearchViewModelConverter,
        private val filterInteractor: FilterInteractor,
        private val settingsSource: SettingsSource
) : BasePaginationPresenter<Any, SearchView>() {

    companion object {
        private const val TAG = "CatalogSearch"
        //search as you type: the pause after the last letter (P10-T3 Q7 b; the site waits 250 ms, but on a
        //phone the gap between letters is often longer, so the list would change under the typing)
        private const val TYPED_SEARCH_DELAY_MS = 500L
    }

    //the filter sheet's lists, read ahead, so the sheet opens with them (P7-T5 Test 81)
    private var sheetCategories: List<FilterCategory>? = null
    private var sheetSorts: List<FilterItem>? = null

    var searchPayload: SearchPayload? = null

    var type: Type = Type.ANIME
    var filters = HashMap<String, MutableList<FilterItem>>()

    //the text the list is searched for. Not one of the sheet's filters: «Сбросить» and the filter icon
    //leave it alone, the field shows it and its ✕ clears it (P10-T2 Q1)
    private var query: String? = null
    //what the field shows, sent or not: a rebuilt screen (a rotation) shows it again (P10-T2 Q18), and a
    //type switch or «Применить» searches it (Q14)
    private var fieldText = ""

    private var settings: CatalogSettings = settingsSource.catalogSettings

    //the list on screen as loaded, drawn again when the settings or the statuses change
    private var shown: List<Any> = emptyList()
    //the user's list statuses for the type shown, by title id: the cards' badges
    private var statuses: Map<Long, RateStatus> = emptyMap()
    //a new list was asked for (newList) and has not come yet: it starts at the top when it comes
    private var newListPending = false

    //characters and people: a new search drops the answer still coming for the old one, as the paginator
    //does for the titles
    private val simpleLoad = loadSlot()
    private val statusLoad = loadSlot()
    //the pause before a search as you type; the next letter starts it again
    private val typedSearch = loadSlot()

    private val supportsPagination = listOf(Type.ANIME, Type.MANGA, Type.RANOBE)

    override fun initData() {
        //a text the field brought back after Android killed the app, reported before this: the field is
        //emptied below, so its search must not run
        typedSearch.cancel()
        viewState.showSettings(settings)

        if (searchPayload != null) {
            onPayloadSearch()
        }

        loadData()
        loadStatuses()
        readFilterSheetAhead()
        viewState.selectType(getTypePos(type))
        viewState.setDefaultEmptyText()
        //an empty field, sent all the same: after Android killed the app the field brings its old text back
        //(and reports it, before this), while the list, made by this new presenter, has no search
        fieldText = ""
        viewState.showQuery(fieldText)
    }

    //back from a title page, where the user may have changed a status; and back to the app. The stored
    //settings too: a backup restored in the settings while the catalog was away (P10-T3 Q17)
    override fun onViewReattached() {
        settingsSource.catalogSettings.takeIf { it != settings }?.let { applySettings(it) }
        loadStatuses()
    }

    override fun loadData() {
        if (isPagination()) loadWithPaginator()
        else loadSimple()
    }

    //the list again: the first load also applied the search it was opened with, which would reset the
    //filters chosen since
    override fun onRetry() {
        loadData()
        loadStatuses()
    }

    //the user's pull: the list again, and the statuses on it
    override fun onRefresh() {
        reloadList()
        loadStatuses()
    }

    //a new search, filters (and a sort), or a type: a new list, which starts at the top (P10-T3 Q18) - when it
    //comes, so the old list stays still under the spinner meanwhile; scrolled as the search began, the old list
    //jumped up first (Q20). A pull, a next page or a changed setting keep the place
    private fun newList() {
        newListPending = true
        reloadList()
    }

    //the list again, the statuses stay. Characters and people keep the list shown under the spinner, as the
    //paged kinds do (BUGS #66)
    private fun reloadList() {
        if (isPagination()) loadWithPaginator()
        else loadSimple(refresh = true)
    }

    private fun loadSimple(refresh: Boolean = false) {
        val request = requestFilters()
        if (request.isEmpty()) {
            simpleLoad.cancel()
            shown = emptyList()
            newListPending = false
            //emptied, not only hidden, as the paged kinds' empty result (Q22)
            viewState.apply { showData(emptyList()); showContent(false); setSimpleEmptyText(); showEmptyView(); onHideLoading() }
        } else simpleLoad.start {
            getSimpleRequestFactory(request)
                    .appendLoadingLogic(viewState, refresh)
                    .subscribe({ showData(true, it) }, {
                        newListPending = false
                        loadErrors(overContent = refresh)(it)
                    })
        }
    }

    private fun loadWithPaginator() {
        super.loadData()
    }

    //every list and page comes here, as loaded: drawn as cards with the settings and the statuses
    override fun showData(show: Boolean, data: List<Any>) {
        shown = data.toList()
        super.showData(show, cards())
        if (show && newListPending) {
            newListPending = false
            viewState.scrollToTop()
        }
    }

    //a new list that found nothing or failed stops waiting for its top: a later page must not jump up
    override fun showEmptyView(show: Boolean) {
        if (show) {
            shown = emptyList()
            newListPending = false
        }
        super.showEmptyView(show)
    }

    override fun showError(throwable: Throwable) {
        newListPending = false
        super.showError(throwable)
    }

    override fun showEmptyError(show: Boolean, throwable: Throwable?) {
        if (show) newListPending = false
        super.showEmptyError(show, throwable)
    }

    private fun cards(): List<Any> = converter.convert(shown, settings, statuses)

    //the cards on screen again, after a setting or the statuses changed
    private fun redraw() {
        if (shown.isNotEmpty()) viewState.showData(cards())
    }

    /**
     * The user's statuses, for the badges: one request with the user's whole list of this type
     * (CODE_CONVENTIONS.md §10). None for a guest (no request), for characters and people, or with the
     * badge hidden. A failed read leaves the cards without badges; the list stays.
     */
    private fun loadStatuses() {
        if (!settings.showStatus || !isPagination()) {
            statusLoad.cancel()
            return
        }
        statusLoad.start {
            interactor.getMyStatuses(type)
                    .subscribe({
                        statuses = it
                        redraw()
                    }, { Log.w(TAG, "the list statuses were not read", it) })
        }
    }

    fun onFilterClicked() {
        viewState.showFilter(type, filters, sheetCategories, sheetSorts)
    }

    //for the type shown; a sheet opened before they come loads them itself
    private fun readFilterSheetAhead() {
        sheetCategories = null
        sheetSorts = null
        if (!isPagination()) return
        val forType = type
        filterInteractor.getFilters(forType)
                .zipWith(filterInteractor.getSortFilters(forType), BiFunction { categories: List<FilterCategory>, sorts: List<FilterItem> -> categories to sorts })
                .subscribe({ (categories, sorts) ->
                    if (type == forType) {
                        sheetCategories = categories
                        sheetSorts = sorts
                    }
                }, { Log.w("FilterSheet", "the filter lists were not read ahead", it) })
                .addToDisposables()
    }

    override fun loadNextPage() {
        if (isPagination()) super.loadNextPage()
    }

    override fun getPaginatorRequestFactory(): (Int) -> Single<Page<Any>> {
        return when (type) {
            Type.ANIME -> { page: Int -> interactor.loadAnimeListWithFilters(requestFilters(), page).map { it.map<Any> { titles -> titles } } }
            else -> { page: Int -> interactor.loadMangaListWithFilters(requestFilters(), type, page).map { it.map<Any> { titles -> titles } } }
        }
    }

    private fun getSimpleRequestFactory(request: HashMap<String, MutableList<FilterItem>>): Single<List<Any>> {
        return when (type) {
            Type.CHARACTER -> interactor.loadCharacterListWithFilters(request).map<List<Any>> { it }
            else -> interactor.loadPersonListWithFilters(request).map<List<Any>> { it }
        }
    }

    //what the list is asked for: the sheet's filters, and the search text under the key the query
    //builder sends as `search`
    private fun requestFilters(): HashMap<String, MutableList<FilterItem>> =
            HashMap(filters).apply {
                query?.let { put(SearchConstants.SEARCH, mutableListOf(FilterItem(SearchConstants.SEARCH, it, null))) }
            }

    private fun isPagination(): Boolean = supportsPagination.contains(type)

    fun onTypeClicked() = viewState.showTypeSheet(getTypePos(type))

    fun onTypeChanged(newTypePos: Int) {
        val newType = getType(newTypePos)
        //the type the sheet marks as current, picked again
        if (newType == type) return
        this.type = newType

        destroyPaginator()
        simpleLoad.cancel()
        shown = emptyList()
        //the old type's: anime and manga ids are separate namespaces
        statuses = emptyMap()

        viewState.apply {
            selectType(newTypePos)
            showData(emptyList())
            showContent(true)
            hideEmptyView()
            setDefaultEmptyText()
            if (supportsPagination.contains(type)) showFilterButton()
            else hideFilterButton()
        }

        //the sheet's filters are the old type's; the field's text stays, searched in the new type (P10-T2 Q2)
        filters.clear()
        takeFieldText()
        newList()
        loadStatuses()
        readFilterSheetAhead()
        showFilterIcon()
    }

    private fun getType(pos: Int): Type {
        return when (pos) {
            0 -> Type.ANIME
            1 -> Type.MANGA
            2 -> Type.RANOBE
            3 -> Type.CHARACTER
            4 -> Type.PERSON
            else -> Type.ANIME
        }
    }

    private fun getTypePos(type: Type): Int {
        return when (type) {
            Type.ANIME -> 0
            Type.MANGA -> 1
            Type.RANOBE -> 2
            Type.CHARACTER -> 3
            Type.PERSON -> 4
            else -> 0
        }
    }

    fun onFilterSelected(appliedFilters: HashMap<String, MutableList<FilterItem>>) {
        filters = appliedFilters
        takeFieldText()
        newList()
        showFilterIcon()
    }

    //a list reloaded by the user's choice (a type, «Применить») searches what the field shows, sent with
    //the keyboard's key or not: a text typed and not sent stayed in the field while the list ignored it
    //(P10-T2 Q14). A search still waiting for a pause in typing is this one
    private fun takeFieldText() {
        typedSearch.cancel()
        query = fieldText.takeIf { it.isNotBlank() }
    }

    //whether the sheet's filters narrow the list, after every change (BUGS #16a). The search text does not
    //count: the field shows it (P10-T2 Q1; it did while «Сбросить» was the only way to clear it, P7-T5 Q22)
    private fun showFilterIcon() = viewState.updateFilterIcon(filters.isEmpty())

    private fun onPayloadSearch() {
        searchPayload?.let {
            if (it.genre != null) onGenreSearch(it.genre)
            else if (it.studioId != null) onStudioSearch(it.studioId, it.studioName)
        }
        viewState.addBackButton()
        showFilterIcon()
    }

    //the keyboard's search key, at once, whatever the pause; the field's text came already (onQueryTextChanged)
    fun onQuerySearch(query: String?) {
        typedSearch.cancel()
        this.query = query
        newList()
    }

    //every change of the field's text, sent back so a rebuilt screen (a rotation) shows it, sent or not
    //(P10-T2 Q18). The same text again (a restored field, or one set by showQuery) changes nothing. An
    //empty or blank field (its ✕, or the text deleted) drops the search at once: the keyboard's search key
    //does nothing on it, so this is how a search is dropped. Any other text is searched after a pause in
    //typing, unless «Искать при вводе» is off or the list already shows it (P10-T3 Q7 b; the site searches
    //as you type too, any length). Each letter starts the pause again
    fun onQueryTextChanged(text: String) {
        if (text == fieldText) return
        fieldText = text
        viewState.showQuery(text)
        typedSearch.cancel()
        when {
            text.isBlank() -> if (query != null) {
                query = null
                newList()
            }
            settings.searchAsYouType && text.trim() != query?.trim() -> typedSearch.start {
                Completable.timer(TYPED_SEARCH_DELAY_MS, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                        .subscribe {
                            query = text
                            newList()
                        }
            }
        }
    }

    fun onSettingsClicked() = viewState.showSettingsSheet(settings, interactor.isSignedIn())

    //a change in the «Отображение» sheet, applied at once: the grid behind the sheet changes as the user
    //picks, with no «Применить» (P10-T3)
    fun onSettingsChanged(newSettings: CatalogSettings) {
        if (newSettings == settings) return
        settingsSource.catalogSettings = newSettings
        val statusSwitched = newSettings.showStatus != settings.showStatus
        applySettings(newSettings)
        if (statusSwitched) loadStatuses()
    }

    //the grid and the cards in [newSettings]; the statuses are its caller's
    private fun applySettings(newSettings: CatalogSettings) {
        settings = newSettings
        viewState.showSettings(newSettings)
        if (!newSettings.searchAsYouType) typedSearch.cancel()
        redraw()
    }

    private fun onStudioSearch(studioId: Long, name: String?) {
        val filterItem = FilterItem(SearchConstants.STUDIO, studioId.toString(), name)
        putFilter(filterItem)
    }

    private fun onGenreSearch(genre: GenreV2) {
        //the id already matches the screen it came from - anime and manga genres are separate
        //namespaces, and these were fetched for this title's own entry type
        val filterItem = FilterItem(SearchConstants.GENRE, genre.id.toString(), genre.russianName)
        putFilter(filterItem)
    }

    private fun putFilter(filter: FilterItem) {
        putFilter(filter.action, filter)
    }

    private fun putFilter(key: String, filter: FilterItem) {
        filters[key] = mutableListOf(filter)
    }
}

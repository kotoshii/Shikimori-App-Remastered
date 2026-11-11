package com.gnoemes.shikimori.presentation.presenter.search

import android.util.Log
import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.search.SearchInteractor
import com.gnoemes.shikimori.domain.search.filter.FilterInteractor
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import com.gnoemes.shikimori.entity.common.domain.SearchConstants
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.entity.search.presentation.SearchItem
import com.gnoemes.shikimori.entity.search.presentation.SearchPayload
import com.gnoemes.shikimori.presentation.presenter.base.BasePaginationPresenter
import com.gnoemes.shikimori.presentation.presenter.search.converter.SearchViewModelConverter
import com.gnoemes.shikimori.presentation.view.search.SearchView
import com.gnoemes.shikimori.presentation.presenter.base.appendLoadingLogic
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

//TODO studios
@InjectViewState
class SearchPresenter @Inject constructor(
        private val interactor: SearchInteractor,
        private val converter: SearchViewModelConverter,
        private val filterInteractor: FilterInteractor
) : BasePaginationPresenter<SearchItem, SearchView>() {

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

    private val supportsPagination = listOf(Type.ANIME, Type.MANGA, Type.RANOBE)

    override fun initData() {
        if (searchPayload != null) {
            onPayloadSearch()
        }

        loadData()
        readFilterSheetAhead()
        viewState.selectType(getTypePos(type))
        viewState.setDefaultEmptyText()
        //an empty field, sent all the same: after Android killed the app the field brings its old text back
        //(and reports it, before this), while the list, made by this new presenter, has no search
        fieldText = ""
        viewState.showQuery(fieldText)
    }

    override fun loadData() {
        if (isPagination()) loadWithPaginator()
        else loadSimple()
    }

    //the list again: the first load also applied the search it was opened with, which would reset the
    //filters chosen since
    override fun onRetry() = loadData()

    //characters and people keep the list shown under the spinner, as the paged kinds do (BUGS #66)
    override fun onRefresh() {
        if (isPagination()) super.onRefresh()
        else loadSimple(refresh = true)
    }

    private fun loadSimple(refresh: Boolean = false) {
        val request = requestFilters()
        if (request.isEmpty()) {
            viewState.apply { showContent(false); setSimpleEmptyText(); showEmptyView(); onHideLoading() }
        } else {
            getSimpleRequestFactory(request)
                    .appendLoadingLogic(viewState, refresh)
                    .subscribe({ showData(true, it) }, loadErrors(overContent = refresh))
                    .addToDisposables()
        }
    }

    private fun loadWithPaginator() {
        super.loadData()
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

    override fun getPaginatorRequestFactory(): (Int) -> Single<Page<SearchItem>> {
        return when (type) {
            Type.ANIME -> { page: Int -> interactor.loadAnimeListWithFilters(requestFilters(), page).map { it.map(converter::apply) } }
            else -> { page: Int -> interactor.loadMangaListWithFilters(requestFilters(), type, page).map { it.map(converter::apply) } }
        }
    }

    private fun getSimpleRequestFactory(request: HashMap<String, MutableList<FilterItem>>): Single<List<SearchItem>> {
        return when (type) {
            Type.CHARACTER -> interactor.loadCharacterListWithFilters(request).map(converter)
            else -> interactor.loadPersonListWithFilters(request).map(converter)
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
        onRefresh()
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
        onRefresh()
        showFilterIcon()
    }

    //a list reloaded by the user's choice (a type, «Применить») searches what the field shows, sent with
    //the keyboard's key or not: a text typed and not sent stayed in the field while the list ignored it
    //(P10-T2 Q14)
    private fun takeFieldText() {
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

    //the keyboard's search key; the field's text came already (onQueryTextChanged)
    fun onQuerySearch(query: String?) {
        this.query = query
        onRefresh()
    }

    //every change of the field's text, sent back so a rebuilt screen (a rotation) shows it, sent or not
    //(P10-T2 Q18). The same text again (a restored field, or one set by showQuery) changes nothing. An
    //empty or blank field (its ✕, or the text deleted) drops the search: the keyboard's search key does
    //nothing on it, so this is how a search is dropped
    fun onQueryTextChanged(text: String) {
        if (text == fieldText) return
        fieldText = text
        viewState.showQuery(text)
        if (text.isBlank() && query != null) {
            query = null
            onRefresh()
        }
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
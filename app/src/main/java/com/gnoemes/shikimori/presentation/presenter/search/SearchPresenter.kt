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

    private val supportsPagination = listOf(Type.ANIME, Type.MANGA, Type.RANOBE)

    override fun initData() {
        if (searchPayload != null) {
            onPayloadSearch()
        }

        loadData()
        readFilterSheetAhead()
        viewState.selectType(getTypePos(type))
        viewState.setDefaultEmptyText()
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
        if (filters.isEmpty()) {
            viewState.apply { showContent(false); setSimpleEmptyText(); showEmptyView(); onHideLoading() }
        } else {
            getSimpleRequestFactory()
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

    override fun getPaginatorRequestFactory(): (Int) -> Single<List<SearchItem>> {
        return when (type) {
            Type.ANIME -> { page: Int -> interactor.loadAnimeListWithFilters(filters, page).map(converter) }
            else -> { page: Int -> interactor.loadMangaListWithFilters(filters, type, page).map(converter) }
        }
    }

    private fun getSimpleRequestFactory(): Single<List<SearchItem>> {
        return when (type) {
            Type.CHARACTER -> interactor.loadCharacterListWithFilters(filters).map(converter)
            else -> interactor.loadPersonListWithFilters(filters).map(converter)
        }
    }

    private fun isPagination(): Boolean = supportsPagination.contains(type)

    fun onTypeChanged(newTypePos: Int) {
        this.type = getType(newTypePos)

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

        filters.clear()
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
        onRefresh()
        showFilterIcon()
    }

    //whether anything narrows the list, after every change. The search text counts: «Сбросить» in the
    //sheet is the only way to clear it, and the icon shows where to look (BUGS #16a, P7-T5 Q22)
    private fun showFilterIcon() = viewState.updateFilterIcon(filters.isEmpty())

    private fun onPayloadSearch() {
        searchPayload?.let {
            if (it.genre != null) onGenreSearch(it.genre)
            else if (it.studioId != null) onStudioSearch(it.studioId, it.studioName)
        }
        viewState.addBackButton()
        showFilterIcon()
    }

    fun onQuerySearch(query: String?) {
        val filterItem = FilterItem(SearchConstants.SEARCH, query, null)
        putFilter(filterItem)
        onRefresh()
        showFilterIcon()
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
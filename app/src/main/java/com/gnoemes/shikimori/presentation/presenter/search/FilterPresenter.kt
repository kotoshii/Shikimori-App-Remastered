package com.gnoemes.shikimori.presentation.presenter.search

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.search.filter.FilterInteractor
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.SearchConstants
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterAction
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.entity.search.presentation.FilterWithButtonsViewModel
import com.gnoemes.shikimori.presentation.presenter.base.BaseFilterPresenter
import com.gnoemes.shikimori.presentation.presenter.search.converter.FilterViewModelConverter
import com.gnoemes.shikimori.presentation.view.search.filter.FilterView
import com.gnoemes.shikimori.utils.clearAndAddAll
import javax.inject.Inject

@InjectViewState
open class FilterPresenter @Inject constructor(
        private val interactor: FilterInteractor,
        private val converter: FilterViewModelConverter
) : BaseFilterPresenter<FilterView>(interactor) {

    private val items = mutableListOf<Any>()

    private val sortItems = mutableListOf<FilterItem>()

    override fun initData() {
        loadSortFilters()
        onFiltersChanged()
    }

    private fun loadSortFilters() {
        sortsOfThisType()
                .doOnSuccess { sortItems.clearAndAddAll(it) }
                .subscribe({ showSort() }, this::processErrors)
                .addToDisposables()
    }

    override fun loadData() {
        loadCategories()
                .map { converter.convert(it, appliedFilters) }
                .subscribe(this::setData, this::processErrors)
                .addToDisposables()
    }

    private fun setData(it: List<Any>) {
        items.clearAndAddAll(it)
        viewState.showData(it)
    }

    //ranobe sorts as manga does: it has chapters and volumes, not episodes, and the api answers an
    //episodes sort for it with 422 (BUGS #10)
    private fun sortsOfThisType() = when (type) {
        Type.MANGA, Type.RANOBE -> interactor.getMangaSortFilters()
        else -> interactor.getAnimeSortFilters()
    }

    fun onSortClicked() {
        viewState.showSortFilters(sortItems, chosenSort())
    }

    fun onSortPicked(value: String) {
        sortItems.find { it.value == value }?.let { onSortChanged(it) }
    }

    //the sort chosen, else the default. Only a chosen sort is in the filters: the default stored there
    //counted as a filter for «Сбросить» and the catalog's filter button (BUGS #16a)
    private fun chosenSort(): String =
            appliedFilters[SearchConstants.ORDER]?.firstOrNull()?.value ?: SearchConstants.DEFAULT_ORDER.toString()

    private fun showSort() {
        val chosen = chosenSort()
        val item = sortItems.find { it.value == chosen } ?: sortItems.firstOrNull() ?: return
        viewState.setSortFilterText(item.localizedText!!)
    }

    fun onSortChanged(newSort: FilterItem) {
        if (newSort.value == SearchConstants.DEFAULT_ORDER.toString()) appliedFilters.remove(SearchConstants.ORDER)
        else appliedFilters[SearchConstants.ORDER] = mutableListOf(newSort)
        showSort()
        onFiltersChanged()
    }

    fun onFilterAction(type: FilterType, action: FilterAction) {
        when (action) {
            is FilterAction.Clear -> clearSection(type.value)
            is FilterAction.Invert -> invertSection(type.value)
            is FilterAction.SelectAll -> selectSection(type.value)
            is FilterAction.ShowNested -> showNested(type)
        }
    }

    //a panel gets only its own filter and hands back only that: with the whole map, unpicking the last
    //genre came back as "no genre key" and left the old genres in (BUGS #14), and the panel's «Сбросить»
    //counted the other filters
    private fun showNested(type: FilterType) {
        val own = HashMap<String, MutableList<FilterItem>>()
        appliedFilters[type.value]?.let { own[type.value] = it }
        when (type) {
            FilterType.GENRE -> viewState.showGenresDialog(super.type, own)
            FilterType.SEASON -> viewState.showSeasonsDialog(super.type, own)
            else -> Unit
        }
    }

    override fun onResetClicked() {
        super.onResetClicked()
        showSort()
    }

    private fun clearSection(key: String) {
        appliedFilters.remove(key)
        onFiltersChanged()
    }

    private fun selectSection(key: String) {
        appliedFilters[key] = items
                .asSequence()
                .filter { it is FilterWithButtonsViewModel }
                .map { it as FilterWithButtonsViewModel }
                .find { it.type.value == key }
                ?.let { it.filters }
                ?.map { FilterItem(key, it.value.replace("!", ""), it.text) }
                ?.toMutableList() ?: mutableListOf()
        onFiltersChanged()
    }

    private fun invertSection(key: String) {
        appliedFilters[key] = items
                .asSequence()
                .filter { it is FilterWithButtonsViewModel }
                .map { it as FilterWithButtonsViewModel }
                .find { it.type.value == key }
                ?.let { it.filters }
                ?.filter { it.state != FilterViewModel.STATE.DEFAULT }
                ?.map { FilterItem(key, it.value.invertValue(), it.text) }
                ?.toMutableList() ?: mutableListOf()
        onFiltersChanged()
    }

    fun onNestedFilterCallback(key: String?, newAppliedFilters: HashMap<String, MutableList<FilterItem>>) {
        if (key == null) return
        val chosen = newAppliedFilters[key]
        if (chosen.isNullOrEmpty()) appliedFilters.remove(key)
        else appliedFilters[key] = chosen
        onFiltersChanged()
    }

}
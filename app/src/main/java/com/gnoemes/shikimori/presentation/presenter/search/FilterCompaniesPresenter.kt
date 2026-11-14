package com.gnoemes.shikimori.presentation.presenter.search

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.search.filter.FilterInteractor
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.search.presentation.FilterEntryViewModel
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.presentation.presenter.base.BaseFilterPresenter
import com.gnoemes.shikimori.presentation.presenter.search.converter.FilterViewModelConverter
import com.gnoemes.shikimori.presentation.view.search.filter.companies.FilterCompaniesView
import javax.inject.Inject

/**
 * The «Студии» panel for anime, «Издатели» for manga and ranobe: a field, the chosen ones as tags, then
 * the whole list, searched on the phone (P10-T4 Q4 a). Several may be chosen; the api then wants all of
 * them on a title at once (Q5 a), and the panel says so. The tags come from the filters themselves, not
 * from the list, so a chosen studio shows while the list loads or after it failed (Q11). A tag goes with
 * its ✕ only, as a typed season's does: a tap on it does nothing (the user, 2026-11-14).
 */
@InjectViewState
class FilterCompaniesPresenter @Inject constructor(
        private val interactor: FilterInteractor,
        private val converter: FilterViewModelConverter
) : BaseFilterPresenter<FilterCompaniesView>(interactor) {

    private val filter: FilterType
        get() = FilterType.companyFor(type)

    //the whole list once loaded, by name; null until then
    private var companies: List<FilterItem>? = null
    private var query = ""

    //a list kept from an earlier opening shows at once: through the background load it flashed the
    //spinner each time, though nothing was asked again (P10-T4 Q16)
    override fun initData() {
        companies = interactor.getKeptCompanies(filter)
        super.initData()
        if (companies == null) loadCompanies()
    }

    private fun loadCompanies() {
        viewState.hideNetworkView()
        viewState.showListLoading(true)
        interactor.getCompanies(filter)
                .doFinally { viewState.showListLoading(false) }
                .subscribe({
                    companies = it
                    loadData()
                }, this::processErrors)
                .addToDisposables()
    }

    fun onRetryClicked() = loadCompanies()

    fun onTagRemoved(tag: FilterEntryViewModel) {
        removeFromSelected(filter.value, FilterItem(filter.value, tag.value, tag.text))
        onFiltersChanged()
    }

    //every letter typed: the rows again, from the loaded list
    fun onQueryChanged(text: String) {
        val newQuery = text.trim()
        if (newQuery == query) return
        query = newQuery
        loadData()
    }

    override fun loadData() {
        val chosen = appliedFilters[filter.value]
        val rows = companies
                ?.let { list -> if (query.isEmpty()) list else list.filter { it.localizedText.orEmpty().contains(query, ignoreCase = true) } }
                ?.let { converter.convertCompanies(it, chosen) }
        viewState.showCompanies(converter.convertChosen(chosen), rows)
    }
}

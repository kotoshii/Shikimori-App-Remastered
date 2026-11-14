package com.gnoemes.shikimori.presentation.view.search.filter.companies

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.search.presentation.FilterEntryViewModel
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFilterView

interface FilterCompaniesView : BaseFilterView {

    /**
     * The chosen ones as tags, and the list's rows that match the text typed; [rows] is null while the
     * list is not loaded. Only the last call counts: the list is drawn again on every letter typed.
     */
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showCompanies(chosen: List<FilterEntryViewModel>, rows: List<FilterViewModel>?)

    /** A spinner in place of the list while it loads. */
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showListLoading(show: Boolean)
}

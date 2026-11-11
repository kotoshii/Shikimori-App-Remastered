package com.gnoemes.shikimori.presentation.view.search

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.presentation.view.base.fragment.BasePaginationView

interface SearchView : BasePaginationView {

    fun selectType(newTypePos: Int)

    fun setSimpleEmptyText()

    fun setDefaultEmptyText()

    @StateStrategyType(SkipStrategy::class)
    fun showFilter(type: Type, filters: HashMap<String, MutableList<FilterItem>>, categories: List<FilterCategory>?, sorts: List<FilterItem>?)

    /** The sheet of what to search, the current type ([currentPos]) marked. */
    @StateStrategyType(SkipStrategy::class)
    fun showTypeSheet(currentPos: Int)

    /**
     * The search field's text, sent or not: the presenter's, sent again on every change, so a rebuilt screen
     * shows it as it was (P10-T2 Q18).
     */
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showQuery(query: String)

    fun showFilterButton()

    fun hideFilterButton()

    fun addBackButton()

    fun updateFilterIcon(empty: Boolean)
}

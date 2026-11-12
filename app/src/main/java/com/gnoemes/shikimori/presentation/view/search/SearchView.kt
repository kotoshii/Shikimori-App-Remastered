package com.gnoemes.shikimori.presentation.view.search

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.CatalogSettings
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

    /** The «Отображение» settings the grid is drawn with: its column count (P10-T3). */
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showSettings(settings: CatalogSettings)

    /** The «Отображение» sheet with [settings]; the status switch is usable only when [signedIn]. */
    @StateStrategyType(SkipStrategy::class)
    fun showSettingsSheet(settings: CatalogSettings, signedIn: Boolean)

    /** A new list has come: its top, and the bar back (P10-T3 Q18, Q20). Once, when the view is there. */
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun scrollToTop()

    fun showFilterButton()

    fun hideFilterButton()

    fun addBackButton()

    fun updateFilterIcon(empty: Boolean)
}

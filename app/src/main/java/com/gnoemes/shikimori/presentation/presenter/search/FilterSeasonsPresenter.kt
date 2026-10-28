package com.gnoemes.shikimori.presentation.presenter.search

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.search.filter.FilterInteractor
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterEntryViewModel
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.presentation.presenter.base.BaseFilterPresenter
import com.gnoemes.shikimori.presentation.presenter.search.converter.FilterViewModelConverter
import com.gnoemes.shikimori.presentation.view.search.filter.seasons.FilterSeasonsView
import java.util.Locale
import javax.inject.Inject

@InjectViewState
class FilterSeasonsPresenter @Inject constructor(
        interactor: FilterInteractor,
        private val converter: FilterViewModelConverter
) : BaseFilterPresenter<FilterSeasonsView>(interactor) {

    //the season chips: a typed season equal to one of them is that chip
    private var chips: List<FilterItem> = emptyList()

    override fun loadData() =
            panelCategory(FilterType.SEASON)
                    .doOnSuccess { chips = it.filters }
                    .map { converter.convertSeasons(it, appliedFilters) }
                    .doOnSuccess { loadCustomFilters() }
                    .subscribe(this::setData, this::processErrors)
                    .addToDisposables()

    private fun loadCustomFilters() {
        val items = converter.convertCustomSeasons(appliedFilters)
        viewState.showCustomData(items)
    }

    private fun setData(it : List<FilterViewModel>) {
        viewState.showSimpleData(it)
    }

    fun onRemoveCustomFilter(item : FilterEntryViewModel) {
        removeFromSelected(FilterType.SEASON.value, FilterItem(FilterType.SEASON.value, item.value, ""))
        onFiltersChanged()
    }

    /**
     * A typed season, taken only when the api knows its form: any text went out as `season=…`, and the
     * catalog answered 422. One equal to a chip's value is that chip, lit, with the chip's label - it
     * showed twice, the chip and an entry beside it (BUGS #75). A season has one form at a time, as a
     * chip does: typed included or excluded, it takes the place of the other.
     *
     * @return whether it was taken; the input keeps a text that was not
     */
    fun onNewCustomFilter(value : String): Boolean {
        val season = value.toLowerCase(Locale.ROOT)
        if (!SEASON_SYNTAX.matches(season)) return false

        val key = FilterType.SEASON.value
        val base = season.removePrefix("!")
        appliedFilters[key]?.filter { it.value?.removePrefix("!") == base }?.forEach { removeFromSelected(key, it) }
        addToSelected(key, FilterItem(key, season, chips.find { it.value == base }?.localizedText.orEmpty()))
        onFiltersChanged()
        return true
    }

    companion object {
        //the api's season forms (CATALOG.md §3): a season of a year, a year, a range of years, a decade,
        //before 1980 - each excluded with `!`
        private val SEASON_SYNTAX = Regex("!?(ancient|\\d{3}x|\\d{4}(_\\d{4})?|(winter|spring|summer|fall)_\\d{4})")
    }
}
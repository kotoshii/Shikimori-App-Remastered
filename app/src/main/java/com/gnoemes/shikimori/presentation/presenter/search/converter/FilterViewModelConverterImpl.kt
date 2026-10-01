package com.gnoemes.shikimori.presentation.presenter.search.converter

import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.*
import com.gnoemes.shikimori.utils.exist
import javax.inject.Inject

class FilterViewModelConverterImpl @Inject constructor() : FilterViewModelConverter {

    override fun convert(filters: List<FilterCategory>, appliedFilters: HashMap<String, MutableList<FilterItem>>): List<Any> {
        val items = mutableListOf<Any>()
        filters.forEach { items.add(convertCategory(it, appliedFilters)) }

        return items
    }

    private fun convertCategory(category: FilterCategory, appliedFilters: HashMap<String, MutableList<FilterItem>>): Any {
        val applied = appliedFilters[category.filterType.value]

        val items = category.filters.map {
            val statuses = getAppliedStatus(it, applied)
            convertFilter(it, statuses.first, statuses.second)
        }

        return when (category.filterType) {
            FilterType.GENRE -> FilterNestedViewModel(category.categoryLocalized, category.filterType, items, applied.size())
            FilterType.SEASON -> FilterNestedViewModel(category.categoryLocalized, category.filterType, items, applied.size())
            FilterType.RATE -> FilterWithButtonsViewModel(category.categoryLocalized, category.filterType, items, hasDelete = true, hasInvert = true, hasSelectAll = true)
            FilterType.KIND -> FilterWithButtonsViewModel(category.categoryLocalized, category.filterType, items, hasDelete = true, hasInvert = false, hasSelectAll = false)
            else -> FilterWithButtonsViewModel(category.categoryLocalized, category.filterType, items, hasDelete = false, hasInvert = false, hasSelectAll = false)
        }
    }

    /**
     * Three flat sections - audience, genres, themes - in that order, coarse to fine, matching the
     * order the details chips are in.
     *
     * The old layout was a hardcoded nine "main" genres plus everything else in alphabet buckets.
     * v2 groups genres itself, and six of those nine were demographics or renamed, so the grouping
     * is taken from the api instead. The vocabulary arrives already sorted by kind and label
     * (`GenreVocabularySource`), so the order inside a section needs no work here.
     */
    override fun convertGenres(category: FilterCategory, appliedFilters: HashMap<String, MutableList<FilterItem>>): List<Any> {
        val applied = appliedFilters[category.filterType.value]

        //UNKNOWN goes with the genres: a kind shikimori adds later still has to appear somewhere,
        //and a chip in the wrong section beats a chip nobody can find
        val sections = listOf(
                R.string.filter_genres_demographic to setOf(GenreV2.Kind.DEMOGRAPHIC),
                R.string.filter_genres_genres to setOf(GenreV2.Kind.GENRE, GenreV2.Kind.UNKNOWN, null),
                R.string.filter_genres_themes to setOf(GenreV2.Kind.THEME)
        )

        return sections.mapNotNull { (titleRes, kinds) ->
            val filters = category.filters
                    .filter { it.genreKind in kinds }
                    .map {
                        val statuses = getAppliedStatus(it, applied)
                        convertFilter(it, statuses.first, statuses.second)
                    }

            if (filters.isEmpty()) null else FilterGenreSection(titleRes, filters)
        }
    }

    override fun convertSeasons(category: FilterCategory, appliedFilters: HashMap<String, MutableList<FilterItem>>): List<FilterViewModel> {
        val applied = appliedFilters[category.filterType.value]

        return category.filters
                .map {
                    val statuses = getAppliedStatus(it, applied)
                    convertFilter(it, statuses.first, statuses.second)
                }
    }

    override fun convertCustomSeasons(appliedFilters: HashMap<String, MutableList<FilterItem>>): List<Any> {
        return (appliedFilters[FilterType.SEASON.value]
                ?.asSequence()
                ?.filter { it.localizedText.isNullOrBlank() }
                ?.map { FilterEntryViewModel(it.value!!) }
                ?.map { it as Any }
                ?.toMutableList()
                ?: mutableListOf())
                .apply { add(FilterEntryInput(size == 0)) }
    }

    private fun getAppliedStatus(checkItem: FilterItem, applied: MutableList<FilterItem>?): Pair<Boolean, Boolean> {
        val item = applied?.find { checkItem.value?.equals(it.value?.replace("!", ""))!! }
        return Pair(item != null, item != null && item.value!!.contains("!"))
    }

    private fun convertFilter(item: FilterItem, isApplied: Boolean, isInverted: Boolean): FilterViewModel {
        val state = when {
            isInverted -> FilterViewModel.STATE.INVERTED
            isApplied -> FilterViewModel.STATE.SELECTED
            else -> FilterViewModel.STATE.DEFAULT
        }
        val value =
                if (isInverted) "!${item.value}"
                else item.value!!

        return FilterViewModel(state, value, item.localizedText!!)
    }

    private fun List<Any>?.size(): Int = this?.size ?: 0
}
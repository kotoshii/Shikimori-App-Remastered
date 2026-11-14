package com.gnoemes.shikimori.presentation.presenter.search.converter

import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.*
import javax.inject.Inject

class FilterViewModelConverterImpl @Inject constructor() : FilterViewModelConverter {

    override fun convert(filters: List<FilterCategory>, appliedFilters: HashMap<String, MutableList<FilterItem>>): List<Any> =
            filters.map { convertCategory(it, appliedFilters) }

    /**
     * The chosen studios or publishers as tags, from the filters alone: each came with its name (from the
     * panel's list, or with a title page's studio), so they show before the list loads and when it failed
     * (P10-T4 Q11). An excluded one keeps its name and gets the − mark.
     */
    override fun convertChosen(chosen: List<FilterItem>?): List<FilterEntryViewModel> =
            chosen.orEmpty().map {
                val value = it.value!!
                FilterEntryViewModel(value, it.localizedText ?: value.removePrefix("!"), excluded = value.startsWith("!"))
            }

    override fun convertCompanies(items: List<FilterItem>, chosen: List<FilterItem>?): List<FilterViewModel> =
            items.map {
                val statuses = getAppliedStatus(it, chosen)
                convertFilter(it, statuses.first, statuses.second)
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
            FilterType.RATE -> FilterWithButtonsViewModel(category.categoryLocalized, category.filterType, items, hasInvert = true, hasSelectAll = true)
            //the list and the chosen ones are the panel's; the row has the count, as «Жанр» (P10-T4). Until
            //P10-T4 a title page's studio had a row of its own at the top, with its chip (BUGS #13)
            FilterType.STUDIO, FilterType.PUBLISHER -> FilterNestedViewModel(category.categoryLocalized, category.filterType, items, applied.size())
            //its chips are in the panel the genres use (P10-T4)
            FilterType.LICENSOR -> FilterNestedViewModel(category.categoryLocalized, category.filterType, items, applied.size())
            else -> FilterWithButtonsViewModel(category.categoryLocalized, category.filterType, items, hasInvert = false, hasSelectAll = false)
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

            if (filters.isEmpty()) null else FilterGenreSection(titleRes, FilterType.GENRE, filters)
        }
    }

    //all the chips in one block with no title: the chip panel opened for «Лицензия»
    override fun convertSection(category: FilterCategory, appliedFilters: HashMap<String, MutableList<FilterItem>>): List<Any> {
        val applied = appliedFilters[category.filterType.value]
        val filters = category.filters.map {
            val statuses = getAppliedStatus(it, applied)
            convertFilter(it, statuses.first, statuses.second)
        }
        return listOf(FilterGenreSection(null, category.filterType, filters))
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
                ?.map { FilterEntryViewModel(it.value!!, it.value.removePrefix("!"), excluded = it.value.startsWith("!")) }
                ?.map { it as Any }
                ?.toMutableList()
                ?: mutableListOf())
                .apply { add(FilterEntryInput(size == 0)) }
    }

    private fun getAppliedStatus(checkItem: FilterItem, applied: List<FilterItem>?): Pair<Boolean, Boolean> {
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
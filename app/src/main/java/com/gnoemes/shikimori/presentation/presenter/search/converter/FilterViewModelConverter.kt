package com.gnoemes.shikimori.presentation.presenter.search.converter

import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.entity.search.presentation.FilterEntryViewModel
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel

interface FilterViewModelConverter  {

    fun convert(filters : List<FilterCategory>, appliedFilters : HashMap<String, MutableList<FilterItem>>) : List<Any>

    fun convertGenres(category: FilterCategory, appliedFilters: HashMap<String, MutableList<FilterItem>>): List<Any>

    /** The category's chips as one untitled block, for the chip panel opened for another row than «Жанр». */
    fun convertSection(category: FilterCategory, appliedFilters: HashMap<String, MutableList<FilterItem>>): List<Any>

    fun convertSeasons(category: FilterCategory, appliedFilters: HashMap<String, MutableList<FilterItem>>): List<FilterViewModel>

    fun convertCustomSeasons(appliedFilters: HashMap<String, MutableList<FilterItem>>): List<Any>

    /** The chosen studios or publishers as tags, from the filters alone. */
    fun convertChosen(chosen: List<FilterItem>?): List<FilterEntryViewModel>

    /** The «Студии» / «Издатели» panel's rows, each marked as [chosen] has it. */
    fun convertCompanies(items: List<FilterItem>, chosen: List<FilterItem>?): List<FilterViewModel>

}
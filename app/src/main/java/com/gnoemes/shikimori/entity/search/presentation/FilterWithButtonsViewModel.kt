package com.gnoemes.shikimori.entity.search.presentation

import com.gnoemes.shikimori.entity.search.domain.FilterType

data class FilterWithButtonsViewModel(
        val categoryLocalised : String,
        val type : FilterType,
        val filters : List<FilterViewModel>,
        //every row has a clear button once a chip is picked; only «Список» and «Тип» had it (BUGS #16d)
        val hasInvert : Boolean,
        val hasSelectAll : Boolean
)
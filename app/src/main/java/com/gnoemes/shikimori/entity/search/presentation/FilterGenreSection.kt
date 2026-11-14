package com.gnoemes.shikimori.entity.search.presentation

import androidx.annotation.StringRes
import com.gnoemes.shikimori.entity.search.domain.FilterType

/**
 * One block of chips in the «Жанры» panel - demographic, genres or themes - or the one untitled block of
 * the same panel opened for «Лицензия» ([titleRes] null). [type]: whose chips they are.
 *
 * Replaces the old "main nine + alphabet buckets" split: v2 groups genres itself, and the nine
 * hardcoded "main" ones were mostly demographics, which now have a section of their own.
 */
data class FilterGenreSection(
        @StringRes val titleRes: Int?,
        val type: FilterType,
        val filters: List<FilterViewModel>
)

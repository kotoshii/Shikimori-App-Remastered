package com.gnoemes.shikimori.entity.search.presentation

import androidx.annotation.StringRes

/**
 * One titled block of genre chips in the filter sheet - demographic, genres or themes.
 *
 * Replaces the old "main nine + alphabet buckets" split: v2 groups genres itself, and the nine
 * hardcoded "main" ones were mostly demographics, which now have a section of their own.
 */
data class FilterGenreSection(
        @StringRes val titleRes: Int,
        val filters: List<FilterViewModel>
)

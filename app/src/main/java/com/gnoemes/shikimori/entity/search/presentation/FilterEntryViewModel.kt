package com.gnoemes.shikimori.entity.search.presentation

/**
 * A tag with ✕: a typed season (its [value] is its text, `!2013` for an excluded one) or a chosen studio or
 * publisher (the id as [value], `!11` excluded; the name as [text], and [excluded] for the − mark).
 */
data class FilterEntryViewModel(
        val value: String,
        val text: String = value,
        val excluded: Boolean = false
)
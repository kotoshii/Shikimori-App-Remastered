package com.gnoemes.shikimori.entity.search.domain

/**
 * The filter sheet's rows, each by its REST param ([value]). [singleValue]: the api takes one value and no
 * `!` exclusion, so a tap replaces the choice and a long press does nothing.
 */
enum class FilterType(val value : String, val singleValue: Boolean = false) {

    GENRE("genre"),
    KIND("kind"),
    STATUS("status"),
    DURATION("duration"),
    RATE("mylist"),
    SEASON("season"),
    //the api's score, at least N: one whole number 1-9 (`score=8,9` and `!8` → 422; CATALOG.md §8)
    SCORE("score", singleValue = true),
    STUDIO("studio"),
    AGE_RATING("rating"),
    //anime only: what the anime was made from (manga, light novel, game...)
    ORIGIN("origin"),

}
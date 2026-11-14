package com.gnoemes.shikimori.entity.search.domain

import com.gnoemes.shikimori.entity.common.domain.Type

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
    //manga and ranobe: the magazine or publisher. Several mean all at once, as studios do
    PUBLISHER("publisher"),
    AGE_RATING("rating"),
    //anime only: what the anime was made from (manga, light novel, game...)
    ORIGIN("origin");

    companion object {
        /** «Студия» for anime, «Издатель» for manga and ranobe: the row and the panel of [type]. */
        fun companyFor(type: Type): FilterType = if (type == Type.ANIME) STUDIO else PUBLISHER
    }

}
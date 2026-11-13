package com.gnoemes.shikimori.entity.search.domain

enum class FilterType(val value : String) {

    GENRE("genre"),
    KIND("kind"),
    STATUS("status"),
    DURATION("duration"),
    RATE("mylist"),
    SEASON("season"),
    SCORE("score"),
    STUDIO("studio"),
    AGE_RATING("rating"),
    //anime only: what the anime was made from (manga, light novel, game...)
    ORIGIN("origin"),

}
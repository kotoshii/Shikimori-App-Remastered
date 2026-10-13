package com.gnoemes.shikimori.entity.common.domain

import org.joda.time.DateTime

/**
 * A season and its year, by the rule of shikimori's season filter (Rails `by_season.rb`): calendar
 * quarters starting 9 days early, so winter 2027 runs from 23 December 2026 to 22 March 2027. A
 * date's season name then matches the catalog chip that finds the title.
 */
data class SeasonYear(val season: SearchConstants.SEASONS, val year: Int) {

    /** The catalog's `season` value, «winter_2027». */
    val value: String
        get() = "${season}_$year"

    companion object {
        fun of(date: DateTime): SeasonYear {
            val shifted = date.plusDays(9)
            val season = when (shifted.monthOfYear) {
                in 1..3 -> SearchConstants.SEASONS.WINTER
                in 4..6 -> SearchConstants.SEASONS.SPRING
                in 7..9 -> SearchConstants.SEASONS.SUMMER
                else -> SearchConstants.SEASONS.FALL
            }
            return SeasonYear(season, shifted.year)
        }
    }
}

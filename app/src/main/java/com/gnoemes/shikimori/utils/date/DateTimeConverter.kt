package com.gnoemes.shikimori.utils.date

import com.gnoemes.shikimori.entity.common.domain.SeasonYear
import org.joda.time.DateTime

interface DateTimeConverter {

    fun convertCalendarDateToString(dateTime: DateTime?): String

    fun convertAnimeSeasonToString(dateTime: DateTime?): String

    /** «Зима 2027». */
    fun convertSeasonToString(season: SeasonYear): String

    fun convertToFullHumanDateString(dateTime: DateTime?): String?

    fun convertHistoryDateToString(actionDate: DateTime): String

    fun convertDateAgoToString(actionDate: DateTime): String

    fun convertShortMonth(dateTime: DateTime) : String
}
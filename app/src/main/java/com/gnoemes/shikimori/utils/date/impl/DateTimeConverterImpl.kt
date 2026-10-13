package com.gnoemes.shikimori.utils.date.impl

import com.gnoemes.shikimori.entity.common.domain.SearchConstants
import com.gnoemes.shikimori.entity.common.domain.SeasonYear
import com.gnoemes.shikimori.utils.date.DateTimeConverter
import com.gnoemes.shikimori.utils.date.DateTimeResourceProvider
import com.gnoemes.shikimori.utils.date.DateTimeUtils
import com.gnoemes.shikimori.utils.firstUpperCase
import org.joda.time.*
import org.joda.time.format.DateTimeFormat
import javax.inject.Inject

//TODO shitty code, refactor sometime
class DateTimeConverterImpl @Inject constructor(
        private val resourceProvider: DateTimeResourceProvider,
        private val dateUtils: DateTimeUtils
) : DateTimeConverter {

    override fun convertCalendarDateToString(dateTime: DateTime?): String {
        val dateFormatter = DateTimeFormat.forPattern("d MMMM")
        val weekFormatter = DateTimeFormat.forPattern("EEEE")

        return when {
            dateUtils.isToday(dateTime) -> String.format(resourceProvider.calendarDateFormat, resourceProvider.todayMessage, dateFormatter.print(dateTime))
            dateUtils.isTomorrow(dateTime) -> String.format(resourceProvider.calendarDateFormat, resourceProvider.tomorrowMessage, dateFormatter.print(dateTime))
            else -> String.format(resourceProvider.calendarDateFormat, weekFormatter.print(dateTime).firstUpperCase(), dateFormatter.print(dateTime).firstUpperCase())
        }
    }

    override fun convertAnimeSeasonToString(dateTime: DateTime?): String =
            dateTime?.let { convertSeasonToString(SeasonYear.of(it)) } ?: "xxx"

    override fun convertSeasonToString(season: SeasonYear): String {
        val name = when (season.season) {
            SearchConstants.SEASONS.WINTER -> resourceProvider.winterString
            SearchConstants.SEASONS.SPRING -> resourceProvider.springString
            SearchConstants.SEASONS.SUMMER -> resourceProvider.summerString
            SearchConstants.SEASONS.FALL -> resourceProvider.fallString
        }
        return "$name ${season.year}"
    }

    override fun convertToFullHumanDateString(dateTime: DateTime?): String? {
        if (dateTime == null) {
            return null
        }

        val dateFormatter = DateTimeFormat.forPattern("dd MMMM yyyy")
        return dateFormatter.print(dateTime)
    }

    override fun convertHistoryDateToString(actionDate: DateTime): String {
        if (dateUtils.isToday(actionDate)) return resourceProvider.todayMessage
        if (dateUtils.isYesterday(actionDate)) return resourceProvider.yesterdayMessage

        val gap = Gap(dateUtils.nowDateTime, actionDate)
        val text = when {
            gap.months == 0 -> resourceProvider.getWeeksAgoString(gap.calendarWeeks)
            dateUtils.isSameYear(actionDate) || gap.years == 0 -> resourceProvider.getMonthsAgoString(gap.months, false)
            else -> resourceProvider.getYearsAgoString(gap.years)
        }
        return text.firstUpperCase()!!
    }

    override fun convertDateAgoToString(actionDate: DateTime): String {
        val gap = Gap(dateUtils.nowDateTime, actionDate)
        return when {
            dateUtils.isSameDay(actionDate) ->
                if (gap.hours == 0) resourceProvider.getMinutesAgoString(gap.minutes)
                else resourceProvider.getHoursAgoString(gap.hours)
            gap.months == 0 -> resourceProvider.getDaysAgoString(gap.days)
            gap.years == 0 -> resourceProvider.getMonthsAgoString(gap.months, true)
            else -> resourceProvider.getYearsAgoString(gap.years)
        }
    }

    /** How far [date] is from [now], in each unit, without the sign. */
    private class Gap(now: DateTime, date: DateTime) {
        val minutes = Math.abs(Minutes.minutesBetween(now, date).minutes)
        val hours = Math.abs(Hours.hoursBetween(now, date).hours)
        val days = Math.abs(Days.daysBetween(now, date).days)
        val months = Math.abs(Months.monthsBetween(now, date).months)
        val years = Math.abs(Years.yearsBetween(now, date).years)

        /**
         * Calendar weeks, Monday to Sunday: 0 is this week, 1 the week before. Counted from the
         * weeks' Mondays, so the turn of the year does not break it - week-of-year numbers did.
         */
        val calendarWeeks = Math.abs(Weeks.weeksBetween(date.weekStart(), now.weekStart()).weeks)

        private fun DateTime.weekStart(): LocalDate = toLocalDate().withDayOfWeek(DateTimeConstants.MONDAY)
    }

    override fun convertShortMonth(dateTime: DateTime): String = resourceProvider.getMonthString(dateTime.monthOfYear)
}
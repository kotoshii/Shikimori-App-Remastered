package com.gnoemes.shikimori.presentation.presenter.userhistory.conveter

import android.content.Context
import androidx.annotation.StringRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.user.domain.UserHistory
import com.gnoemes.shikimori.entity.user.presentation.UserHistoryHeaderViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserHistoryViewModel
import com.gnoemes.shikimori.utils.date.DateTimeConverter
import com.gnoemes.shikimori.utils.date.DateTimeUtils
import org.joda.time.DateTime
import javax.inject.Inject

class UserHistoryViewModelConverterImpl @Inject constructor(
        private val context: Context,
        private val settings: SettingsSource,
        private val utils: DateTimeUtils,
        private val dateConverter: DateTimeConverter
) : UserHistoryViewModelConverter {

    override fun apply(t: List<UserHistory>): List<UserHistoryViewModel> =
            t.map { convertHistory(it) }

    private fun convertHistory(it: UserHistory): UserHistoryViewModel {
        //an entry can come without a title
        val name = when (val target = it.target) {
            is Anime -> settings.titleName(target.name, target.nameRu)
            is Manga -> settings.titleName(target.name, target.nameRu)
            else -> target?.linkedName
        }

       return UserHistoryViewModel(
                it.id,
                it.description,
                dateConverter.convertDateAgoToString(it.dateCreated),
                it.dateCreated,
                name,
                it.target
        )
    }

    /** The entries under the site's headers: a header wherever the interval changes, so none repeats. */
    override fun groupItems(items: List<UserHistoryViewModel>): List<Any> {
        val now = utils.nowDateTime
        val list = mutableListOf<Any>()
        var previous: HistoryInterval? = null

        items.forEach { item ->
            val interval = HistoryInterval.of(item.actionDateTime, now)
            if (interval != previous) list.add(UserHistoryHeaderViewModel(context.getString(interval.title)))
            previous = interval
            list.add(item)
        }
        return list
    }

    /**
     * The site's history intervals and headers (`user_history_query.rb`, `datetime.intervals`),
     * newest first: each is "older than the one before it, but not older than its own limit".
     */
    private enum class HistoryInterval(@StringRes val title: Int) {
        TODAY(R.string.common_today),
        YESTERDAY(R.string.common_yesterday),
        DURING_WEEK(R.string.history_during_week),
        WEEK(R.string.history_week_ago),
        TWO_WEEKS(R.string.history_two_weeks_ago),
        THREE_WEEKS(R.string.history_three_weeks_ago),
        MONTH(R.string.history_month_ago),
        TWO_MONTHS(R.string.history_two_months_ago),
        THREE_MONTHS(R.string.history_three_months_ago),
        FOUR_MONTHS(R.string.history_four_months_ago),
        FIVE_MONTHS(R.string.history_five_months_ago),
        HALF_YEAR(R.string.history_half_year_ago),
        YEAR(R.string.history_year_ago),
        TWO_YEARS(R.string.history_two_years_ago),
        MANY_YEARS(R.string.history_long_ago);

        companion object {
            fun of(date: DateTime, now: DateTime): HistoryInterval = when {
                date.isBefore(now.minusYears(3)) -> MANY_YEARS
                date.isBefore(now.minusYears(2)) -> TWO_YEARS
                date.isBefore(now.minusYears(1)) -> YEAR
                date.isBefore(now.minusMonths(6)) -> HALF_YEAR
                date.isBefore(now.minusMonths(5)) -> FIVE_MONTHS
                date.isBefore(now.minusMonths(4)) -> FOUR_MONTHS
                date.isBefore(now.minusMonths(3)) -> THREE_MONTHS
                date.isBefore(now.minusMonths(2)) -> TWO_MONTHS
                date.isBefore(now.minusMonths(1)) -> MONTH
                date.isBefore(now.minusWeeks(3)) -> THREE_WEEKS
                date.isBefore(now.minusWeeks(2)) -> TWO_WEEKS
                date.isBefore(now.minusWeeks(1)) -> WEEK
                //the site: before the end of the day before yesterday
                date.isBefore(now.minusDays(1).withTimeAtStartOfDay()) -> DURING_WEEK
                date.isBefore(now.withTimeAtStartOfDay()) -> YESTERDAY
                else -> TODAY
            }
        }
    }
}
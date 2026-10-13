package com.gnoemes.shikimori.domain.calendar

import com.gnoemes.shikimori.data.repository.calendar.CalendarRepository
import com.gnoemes.shikimori.data.repository.rates.RatesRepository
import com.gnoemes.shikimori.entity.calendar.domain.CalendarItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Single
import javax.inject.Inject

class CalendarInteractorImpl @Inject constructor(
        private val repository: CalendarRepository,
        private val rateRepository: RatesRepository
) : CalendarInteractor {

    //the calendar marks only what the user is watching or about to
    override fun getCalendarData(): Single<List<CalendarItem>> =
            repository.getData()
                    .flatMap { calendar ->
                        rateRepository.getMyRates(Type.ANIME, listOf(RateStatus.WATCHING, RateStatus.PLANNED, RateStatus.ON_HOLD))
                                .map { rates -> calendar.map { item -> rates[item.anime.id]?.let { item.copy(status = it.status) } ?: item } }
                    }
                    .applyErrorHandlerAndSchedulers()
}
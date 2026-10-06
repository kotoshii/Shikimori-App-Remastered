package com.gnoemes.shikimori.di.calendar;

import com.gnoemes.shikimori.data.repository.calendar.CalendarRepository;
import com.gnoemes.shikimori.data.repository.calendar.CalendarRepositoryImpl;
import com.gnoemes.shikimori.data.repository.calendar.converter.CalendarResponseConverter;
import com.gnoemes.shikimori.data.repository.calendar.converter.CalendarResponseConverterImpl;
import com.gnoemes.shikimori.domain.calendar.CalendarInteractor;
import com.gnoemes.shikimori.domain.calendar.CalendarInteractorImpl;
import com.gnoemes.shikimori.presentation.presenter.calendar.converter.CalendarViewModelConverter;
import com.gnoemes.shikimori.presentation.presenter.calendar.converter.CalendarViewModelConverterImpl;

import dagger.Binds;
import dagger.Module;

@Module
public interface CalendarModule {

    @Binds
    CalendarInteractor bindCalendarInteractor(CalendarInteractorImpl interactor);

    @Binds
    CalendarRepository bindCalendarRepository(CalendarRepositoryImpl repository);

    @Binds
    CalendarResponseConverter bindCalendarResponseConverter(CalendarResponseConverterImpl converter);

    @Binds
    CalendarViewModelConverter bindCalendarViewModelConverter(CalendarViewModelConverterImpl converter);
}

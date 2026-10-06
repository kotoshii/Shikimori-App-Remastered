package com.gnoemes.shikimori.di.rate;

import com.gnoemes.shikimori.data.local.preference.RateSortSource;
import com.gnoemes.shikimori.data.local.preference.impl.RateSortSourceImpl;
import com.gnoemes.shikimori.di.series.SeriesInteractorModule;
import com.gnoemes.shikimori.di.series.SeriesRepositoryModule;
import com.gnoemes.shikimori.di.series.SeriesUtilModule;
import com.gnoemes.shikimori.di.user.UserInteractorModule;
import com.gnoemes.shikimori.di.user.UserUtilModule;
import com.gnoemes.shikimori.presentation.presenter.rates.converter.RateCountConverter;
import com.gnoemes.shikimori.presentation.presenter.rates.converter.RateCountConverterImpl;

import dagger.Binds;
import dagger.Module;
import dagger.Reusable;

@Module(includes = {
        SyncModule.class,
        SeriesUtilModule.class,
        SeriesRepositoryModule.class,
        SeriesInteractorModule.class,
        UserInteractorModule.class,
        UserUtilModule.class,
        RateInteractorModule.class,
        RateUtilModule.class
})
public interface RateModule {

    @Binds
    @Reusable
    RateCountConverter bindRateCountConverter(RateCountConverterImpl converter);

    @Binds
    @Reusable
    RateSortSource bindRateSortSource(RateSortSourceImpl source);
}
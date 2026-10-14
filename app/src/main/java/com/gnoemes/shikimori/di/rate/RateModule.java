package com.gnoemes.shikimori.di.rate;

import com.gnoemes.shikimori.data.local.preference.RateSortSource;
import com.gnoemes.shikimori.data.local.preference.impl.RateSortSourceImpl;
import com.gnoemes.shikimori.presentation.presenter.rates.converter.RateCountConverter;
import com.gnoemes.shikimori.presentation.presenter.rates.converter.RateCountConverterImpl;

import dagger.Binds;
import dagger.Module;
import dagger.Reusable;

@Module(includes = SeriesAndRatesModule.class)
public interface RateModule {

    @Binds
    @Reusable
    RateCountConverter bindRateCountConverter(RateCountConverterImpl converter);

    @Binds
    @Reusable
    RateSortSource bindRateSortSource(RateSortSourceImpl source);
}

package com.gnoemes.shikimori.di.rate;

import com.gnoemes.shikimori.domain.rates.PinnedRateInteractor;
import com.gnoemes.shikimori.domain.rates.PinnedRateInteractorImpl;

import dagger.Binds;
import dagger.Module;
import dagger.Reusable;

@Module
public interface RateInteractorModule {

    @Binds
    @Reusable
    PinnedRateInteractor bindPinnedRateInteractor(PinnedRateInteractorImpl interactor);
}

package com.gnoemes.shikimori.di.anime;

import com.gnoemes.shikimori.di.rate.RateModule;
import com.gnoemes.shikimori.di.related.RelatedModule;
import com.gnoemes.shikimori.di.studio.StudioUtilModule;

import dagger.Module;

//RateModule brings the series, rates and user modules (SeriesAndRatesModule)
@Module(includes = {
        AnimeUtilModule.class,
        AnimeRepositoryModule.class,
        AnimeInteractorModule.class,
        RateModule.class,
        StudioUtilModule.class,
        RelatedModule.class
})
public interface AnimeModule {
}

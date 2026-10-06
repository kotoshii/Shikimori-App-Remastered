package com.gnoemes.shikimori.di.anime;

import com.gnoemes.shikimori.di.rate.RateInteractorModule;
import com.gnoemes.shikimori.di.rate.RateModule;
import com.gnoemes.shikimori.di.rate.SyncModule;
import com.gnoemes.shikimori.di.related.RelatedModule;
import com.gnoemes.shikimori.di.series.SeriesInteractorModule;
import com.gnoemes.shikimori.di.series.SeriesRepositoryModule;
import com.gnoemes.shikimori.di.series.SeriesUtilModule;
import com.gnoemes.shikimori.di.studio.StudioUtilModule;
import com.gnoemes.shikimori.di.user.UserInteractorModule;
import com.gnoemes.shikimori.di.user.UserUtilModule;

import dagger.Module;

@Module(includes = {
        AnimeUtilModule.class,
        AnimeRepositoryModule.class,
        AnimeInteractorModule.class,
        RateModule.class,
        UserUtilModule.class,
        UserInteractorModule.class,
        StudioUtilModule.class,
        RelatedModule.class,
        RateInteractorModule.class,
        SyncModule.class,
        SeriesUtilModule.class,
        SeriesRepositoryModule.class,
        SeriesInteractorModule.class
})
public interface AnimeModule {
}

package com.gnoemes.shikimori.di.manga

import com.gnoemes.shikimori.di.rate.RateModule
import com.gnoemes.shikimori.di.related.RelatedModule
import dagger.Module

//RateModule brings the series, rates and user modules (SeriesAndRatesModule)
@Module(includes = [
    MangaRepositoryModule::class,
    MangaUtilModule::class,
    MangaInteractorModule::class,
    RateModule::class,
    RelatedModule::class
])
interface MangaModule {
}

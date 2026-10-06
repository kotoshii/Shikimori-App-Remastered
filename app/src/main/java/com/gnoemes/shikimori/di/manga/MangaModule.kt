package com.gnoemes.shikimori.di.manga

import com.gnoemes.shikimori.di.rate.RateModule
import com.gnoemes.shikimori.di.rate.RateInteractorModule
import com.gnoemes.shikimori.di.rate.SyncModule
import com.gnoemes.shikimori.di.related.RelatedModule
import com.gnoemes.shikimori.di.user.UserInteractorModule
import com.gnoemes.shikimori.di.user.UserUtilModule
import dagger.Module

@Module(includes = [
    MangaRepositoryModule::class,
    MangaUtilModule::class,
    MangaInteractorModule::class,
    RateModule::class,
    UserUtilModule::class,
    UserInteractorModule::class,
    RelatedModule::class,
    RateInteractorModule::class,
    SyncModule::class
])
interface MangaModule {
}
package com.gnoemes.shikimori.di.series

import com.gnoemes.shikimori.di.rate.SeriesAndRatesModule
import dagger.Module

@Module(includes = [SeriesAndRatesModule::class])
interface EpisodeModule {
}

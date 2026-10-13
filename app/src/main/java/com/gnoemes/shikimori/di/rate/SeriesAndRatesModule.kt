package com.gnoemes.shikimori.di.rate

import com.gnoemes.shikimori.di.series.SeriesInteractorModule
import com.gnoemes.shikimori.di.series.SeriesRepositoryModule
import com.gnoemes.shikimori.di.series.SeriesUtilModule
import com.gnoemes.shikimori.di.user.UserInteractorModule
import com.gnoemes.shikimori.di.user.UserUtilModule
import dagger.Module

/**
 * What every screen that shows episodes or edits a list entry needs: the series (episodes and
 * translations), the user's rates and pinned rates, the watch progress, and the user. One list,
 * instead of the same eight modules written out in each screen's module.
 */
@Module(includes = [
    PinnedRateModule::class,
    SeriesUtilModule::class,
    SeriesRepositoryModule::class,
    SeriesInteractorModule::class,
    UserInteractorModule::class,
    UserUtilModule::class,
    RateInteractorModule::class,
    RateUtilModule::class
])
interface SeriesAndRatesModule

package com.gnoemes.shikimori.di.rate

import com.gnoemes.shikimori.data.local.db.PinnedRateDbSource
import com.gnoemes.shikimori.data.local.db.impl.PinnedRateDbSourceImpl
import com.gnoemes.shikimori.data.repository.rates.PinnedRateRepository
import com.gnoemes.shikimori.data.repository.rates.PinnedRateRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable

//was SyncModule, after upstream's series sync service, removed in 2019
@Module(includes = [AnimeProgressModule::class])
interface PinnedRateModule {

    @Binds
    @Reusable
    fun bindPinnedSource(source : PinnedRateDbSourceImpl) : PinnedRateDbSource

    @Binds
    @Reusable
    fun bindPinnedRepo(repo : PinnedRateRepositoryImpl) : PinnedRateRepository

}

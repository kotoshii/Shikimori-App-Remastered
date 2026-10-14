package com.gnoemes.shikimori.di.series

import com.gnoemes.shikimori.data.network.AnimeSource
import com.gnoemes.shikimori.data.network.impl.AnimeSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable

@Module
interface SeriesAnimeModule {

    @Binds
    @Reusable
    fun bindAnimeSource(source: AnimeSourceImpl): AnimeSource
}

package com.gnoemes.shikimori.di.series

import com.gnoemes.shikimori.data.network.AnimeSource
import com.gnoemes.shikimori.data.network.ShikicinemaVideoApi
import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.data.network.impl.AnimeSourceImpl
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
class SeriesAnimeModule {

    @Provides
    @Reusable
    fun provideAnimeSource(videoApi: VideoApi, shikicinemaVideoApi: ShikicinemaVideoApi) : AnimeSource {
      return  AnimeSourceImpl(videoApi, shikicinemaVideoApi)
    }
}
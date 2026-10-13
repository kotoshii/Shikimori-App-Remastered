package com.gnoemes.shikimori.di.manga

import com.gnoemes.shikimori.data.repository.manga.MangaRepository
import com.gnoemes.shikimori.data.repository.manga.MangaRepositoryImpl
import dagger.Binds
import dagger.Module

@Module
interface MangaRepositoryModule {

    @Binds
    fun bindMangaRepository(repository : MangaRepositoryImpl) : MangaRepository

}
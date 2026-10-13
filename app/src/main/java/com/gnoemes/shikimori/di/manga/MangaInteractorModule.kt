package com.gnoemes.shikimori.di.manga

import com.gnoemes.shikimori.domain.manga.MangaInteractor
import com.gnoemes.shikimori.domain.manga.MangaInteractorImpl
import dagger.Binds
import dagger.Module

@Module
interface MangaInteractorModule {

    @Binds
    fun bindMangaInteractor(interactor : MangaInteractorImpl) : MangaInteractor
}
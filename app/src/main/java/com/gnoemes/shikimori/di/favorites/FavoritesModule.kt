package com.gnoemes.shikimori.di.favorites

import com.gnoemes.shikimori.di.user.UserInteractorModule
import com.gnoemes.shikimori.di.user.UserUtilModule
import com.gnoemes.shikimori.presentation.presenter.favorites.converter.FavoriteViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.favorites.converter.FavoriteViewModelConverterImpl
import dagger.Binds
import dagger.Module

@Module(includes = [
    UserInteractorModule::class,
    UserUtilModule::class
])
interface FavoritesModule {

    @Binds
    fun bindConverter(converter: FavoriteViewModelConverterImpl): FavoriteViewModelConverter

}
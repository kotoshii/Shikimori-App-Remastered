package com.gnoemes.shikimori.di.friends

import com.gnoemes.shikimori.di.user.UserInteractorModule
import com.gnoemes.shikimori.di.user.UserUtilModule
import com.gnoemes.shikimori.presentation.presenter.friends.converter.FriendsViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.friends.converter.FriendsViewModelConverterImpl
import dagger.Binds
import dagger.Module

@Module(includes = [
    UserInteractorModule::class,
    UserUtilModule::class
])
interface FriendsModule {

    @Binds
    fun provideConverter(converter: FriendsViewModelConverterImpl): FriendsViewModelConverter
}
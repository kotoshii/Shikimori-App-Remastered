package com.gnoemes.shikimori.di.club

import com.gnoemes.shikimori.di.user.UserInteractorModule
import com.gnoemes.shikimori.di.user.UserUtilModule
import com.gnoemes.shikimori.presentation.presenter.clubs.converter.UserClubViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.clubs.converter.UserClubViewModelConverterImpl
import dagger.Binds
import dagger.Module

@Module(includes = [
    UserInteractorModule::class,
    UserUtilModule::class
])
interface UserClubsModule {

    @Binds
    fun provideConverter(converter: UserClubViewModelConverterImpl): UserClubViewModelConverter
}
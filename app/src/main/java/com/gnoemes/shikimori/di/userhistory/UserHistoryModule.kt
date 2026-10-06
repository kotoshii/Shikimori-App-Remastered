package com.gnoemes.shikimori.di.userhistory

import com.gnoemes.shikimori.di.user.UserInteractorModule
import com.gnoemes.shikimori.di.user.UserUtilModule
import com.gnoemes.shikimori.presentation.presenter.userhistory.conveter.UserHistoryViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.userhistory.conveter.UserHistoryViewModelConverterImpl
import dagger.Binds
import dagger.Module

@Module(includes = [
    UserInteractorModule::class,
    UserUtilModule::class
])
interface UserHistoryModule {

    @Binds
    fun bindConverter(converter: UserHistoryViewModelConverterImpl): UserHistoryViewModelConverter
}
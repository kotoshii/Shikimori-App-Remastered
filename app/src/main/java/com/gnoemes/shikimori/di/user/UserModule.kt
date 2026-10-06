package com.gnoemes.shikimori.di.user

import dagger.Module

@Module(includes = [
    UserInteractorModule::class,
    UserUtilModule::class
])
interface UserModule {
}
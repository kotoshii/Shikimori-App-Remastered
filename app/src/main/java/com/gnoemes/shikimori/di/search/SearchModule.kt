package com.gnoemes.shikimori.di.search

import dagger.Module

@Module(includes = [
    SearchRepositoryModule::class,
    SearchInteractorModule::class,
    SearchUtilModule::class
])
interface SearchModule {
}
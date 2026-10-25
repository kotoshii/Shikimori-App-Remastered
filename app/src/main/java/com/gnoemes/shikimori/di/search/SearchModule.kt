package com.gnoemes.shikimori.di.search

import dagger.Module

//FilterModule too: the catalog reads the filter sheet's lists ahead (SearchPresenter)
@Module(includes = [
    SearchRepositoryModule::class,
    SearchInteractorModule::class,
    SearchUtilModule::class,
    FilterModule::class
])
interface SearchModule {
}
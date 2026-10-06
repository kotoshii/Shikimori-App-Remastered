package com.gnoemes.shikimori.di.topic.list

import com.gnoemes.shikimori.di.topic.TopicInteractorModule
import com.gnoemes.shikimori.di.topic.TopicRepositoryModule
import com.gnoemes.shikimori.di.topic.TopicUtilModule
import dagger.Module

@Module(includes = [
    TopicUtilModule::class,
    TopicRepositoryModule::class,
    TopicInteractorModule::class
])
interface TopicListModule {
}
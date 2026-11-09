package com.gnoemes.shikimori.di.topic.list

import com.gnoemes.shikimori.di.topic.TopicInteractorModule
import com.gnoemes.shikimori.di.topic.TopicUtilModule
import dagger.Module

@Module(includes = [
    TopicUtilModule::class,
    TopicInteractorModule::class
])
interface TopicListModule {
}
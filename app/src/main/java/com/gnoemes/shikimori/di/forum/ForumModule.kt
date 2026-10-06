package com.gnoemes.shikimori.di.forum

import com.gnoemes.shikimori.di.topic.TopicInteractorModule
import com.gnoemes.shikimori.di.topic.TopicRepositoryModule
import com.gnoemes.shikimori.di.topic.TopicUtilModule
import com.gnoemes.shikimori.presentation.view.forum.converter.ForumConverter
import com.gnoemes.shikimori.presentation.view.forum.converter.ForumConverterImpl
import dagger.Binds
import dagger.Module

@Module(includes = [
    TopicInteractorModule::class,
    TopicRepositoryModule::class,
    TopicUtilModule::class
])
interface ForumModule {

    @Binds
    fun bindConverter(converter: ForumConverterImpl): ForumConverter
}
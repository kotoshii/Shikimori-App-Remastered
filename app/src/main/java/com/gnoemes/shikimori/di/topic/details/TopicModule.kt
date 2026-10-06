package com.gnoemes.shikimori.di.topic.details

import com.gnoemes.shikimori.di.comment.CommentModule
import com.gnoemes.shikimori.di.topic.TopicInteractorModule
import com.gnoemes.shikimori.di.topic.TopicRepositoryModule
import com.gnoemes.shikimori.di.topic.TopicUtilModule
import dagger.Module

@Module(includes = [
    TopicInteractorModule::class,
    TopicRepositoryModule::class,
    TopicUtilModule::class,
    CommentModule::class
])
interface TopicModule {
}
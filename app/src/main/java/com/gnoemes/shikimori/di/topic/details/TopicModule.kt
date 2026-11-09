package com.gnoemes.shikimori.di.topic.details

import com.gnoemes.shikimori.di.comment.CommentModule
import com.gnoemes.shikimori.di.topic.TopicInteractorModule
import com.gnoemes.shikimori.di.topic.TopicUtilModule
import dagger.Module

@Module(includes = [
    TopicInteractorModule::class,
    TopicUtilModule::class,
    CommentModule::class
])
interface TopicModule {
}
package com.gnoemes.shikimori.di.shikimorimain

import com.gnoemes.shikimori.di.forum.ForumModule
import com.gnoemes.shikimori.presentation.view.forum.ForumFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface ShikimoriMainModule {

    @ContributesAndroidInjector(modules = [ForumModule::class])
    fun forumFragment(): ForumFragment
}
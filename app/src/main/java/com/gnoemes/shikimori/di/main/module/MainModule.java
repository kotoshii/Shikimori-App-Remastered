package com.gnoemes.shikimori.di.main.module;

import com.gnoemes.shikimori.di.base.modules.BaseActivityModule;
import com.gnoemes.shikimori.di.base.scopes.ActivityScope;
import com.gnoemes.shikimori.di.base.scopes.BottomScope;
import com.gnoemes.shikimori.di.bottom.BottomTabModule;
import com.gnoemes.shikimori.presentation.view.bottom.BottomTabContainer;
import com.gnoemes.shikimori.presentation.view.main.MainActivity;

import androidx.appcompat.app.AppCompatActivity;
import dagger.Binds;
import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module(includes = {BaseActivityModule.class,
})
public interface MainModule {

    @BottomScope
    @ContributesAndroidInjector(modules = BottomTabModule.class)
    BottomTabContainer fragmentMenuFragmentContainerInjector();

    @Binds
    @ActivityScope
    AppCompatActivity bindAppCompatActivity(MainActivity mainActivity);
}

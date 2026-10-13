package com.gnoemes.shikimori.di.settings;

import com.gnoemes.shikimori.presentation.view.settings.fragments.SettingsAnimeFragment;
import com.gnoemes.shikimori.presentation.view.settings.fragments.SettingsFragment;
import com.gnoemes.shikimori.presentation.view.settings.fragments.SettingsGeneralFragment;
import com.gnoemes.shikimori.presentation.view.settings.fragments.SettingsThemeFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * The settings pages, each injected in {@code BaseSettingsFragment.onAttach}: every page that
 * extends it must be listed here, or opening it fails.
 */
@Module
public interface SettingsFragmentInjectionModule {

    @ContributesAndroidInjector
    SettingsFragment settingsFragmentInjector();

    @ContributesAndroidInjector
    SettingsGeneralFragment settingsGeneralFragmentInjector();

    @ContributesAndroidInjector
    SettingsAnimeFragment settingsAnimeFragmentInjector();

    @ContributesAndroidInjector
    SettingsThemeFragment settingsThemeFragmentInjector();
}

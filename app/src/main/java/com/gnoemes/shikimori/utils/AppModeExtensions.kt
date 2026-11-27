package com.gnoemes.shikimori.utils

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.gnoemes.shikimori.entity.app.domain.AppMode
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras

//the interface mode lives in the theme preferences, not in the injected SettingsSource: it is read
//by every activity's base class and by the first-launch dialog, before any Dagger scope is ready

private val Context.appModePrefs
    get() = getThemeSharedPreferences()

/** The interface mode; MOBILE until the user answers the first-launch dialog. */
val Context.appMode: AppMode
    get() = appModePrefs.getInt(SettingsExtras.APP_MODE_KEY, AppMode.MOBILE.index)
            .let { index -> AppMode.values().find { it.index == index } ?: AppMode.MOBILE }

/** Whether the first-launch dialog already has an answer, so it must not be shown again. */
val Context.hasStoredAppMode: Boolean
    get() = appModePrefs.contains(SettingsExtras.APP_MODE_KEY)

fun Context.setAppMode(mode: AppMode) {
    appModePrefs.edit().putInt(SettingsExtras.APP_MODE_KEY, mode.index).apply()
}

/** The device itself: a television, or a box running Android TV. */
val Context.isTvDevice: Boolean
    get() {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
        return packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
    }

/** The interface actually shown - the mode the user picked, not what the device says. */
val Context.isTvUi: Boolean
    get() = appMode == AppMode.TV

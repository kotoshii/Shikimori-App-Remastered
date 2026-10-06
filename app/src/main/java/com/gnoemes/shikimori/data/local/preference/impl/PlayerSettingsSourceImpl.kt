package com.gnoemes.shikimori.data.local.preference.impl

import android.content.SharedPreferences
import com.gnoemes.shikimori.data.local.preference.PlayerSettingsSource
import com.gnoemes.shikimori.di.app.annotations.SettingsQualifier
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import javax.inject.Inject

class PlayerSettingsSourceImpl @Inject constructor(
        @SettingsQualifier private val prefs: SharedPreferences
) : PlayerSettingsSource {

    override val isGesturesEnabled: Boolean
        get() = prefs.getBoolean(SettingsExtras.PLAYER_IS_GESTURES_ENABLED, true)

    override val isVolumeAndBrightnessGesturesEnabled: Boolean
        get() = prefs.getBoolean(SettingsExtras.PLAYER_IS_VOLUME_BRIGHTNESS_GESTURES_ENABLED, true)

    override val isVolumeAndBrightnessInverted: Boolean
        get() = prefs.getBoolean(SettingsExtras.PLAYER_IS_VOLUME_AND_BRIGHTNESS_INVERTED, false)

    override val isForwardRewindSlide: Boolean
        get() = prefs.getBoolean(SettingsExtras.PLAYER_IS_FORWARD_REWIND_SLIDE, false)

    override val isOpenLandscape: Boolean
        get() = prefs.getBoolean(SettingsExtras.PLAYER_IS_OPEN_LANDSCAPE, true)

    override val isZoomProportional: Boolean
        get() = prefs.getBoolean(SettingsExtras.PLAYER_IS_ZOOM_PROPORTIONAL, true)

    override val isAutoPip: Boolean
        get() = prefs.getBoolean(SettingsExtras.PLAYER_IS_AUTO_PIP, true)

    override val forwardRewindOffset: Long
        get() = prefs.getLong(SettingsExtras.PLAYER_FORWARD_REWIND_OFFSET, 10000)

    override val forwardRewindOffsetBig: Long
        get() = prefs.getLong(SettingsExtras.PLAYER_FORWARD_REWIND_OFFSET_BIG, 90000)
}
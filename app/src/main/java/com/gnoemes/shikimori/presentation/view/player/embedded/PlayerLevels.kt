package com.gnoemes.shikimori.presentation.view.player.embedded

import android.app.Activity
import android.media.AudioManager
import android.provider.Settings
import com.gnoemes.shikimori.utils.audioManager
import kotlin.math.roundToInt

/**
 * The player's brightness and volume swipes. Each swipe starts from the real level (BUGS #36): the
 * volume keys may have changed the volume since the last swipe, and the window has no brightness of
 * its own until a swipe sets one, so the first swipe starts from the system's.
 */
class PlayerLevels(private val activity: Activity) {

    private val audio = activity.audioManager()

    //the levels while a swipe goes on; null until its first step reads them
    private var brightness: Int? = null
    private var volume: Int? = null

    /** Changes the brightness by [step] percent; the new level, 1–100. */
    fun changeBrightness(step: Int): Int {
        val level = ((brightness ?: readBrightness()) + step).coerceIn(1, 100)
        brightness = level
        activity.window.attributes = activity.window.attributes.apply { screenBrightness = level / 100f }
        return level
    }

    /** Changes the volume by [step] of the system's volume steps; the new level in percent. */
    fun changeVolume(step: Int): Int {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val level = ((volume ?: audio.getStreamVolume(AudioManager.STREAM_MUSIC)) + step).coerceIn(0, max)
        volume = level
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
        return if (level == 0) 0 else (level / (max * 0.01f)).roundToInt()
    }

    /** The swipe is over: the next one reads the levels again. */
    fun onSwipeEnd() {
        brightness = null
        volume = null
    }

    //the window's own level once a swipe has set it, else the system's. That one is 0-255 on most
    //phones and not linear on newer ones, so the start is close rather than exact
    private fun readBrightness(): Int {
        val own = activity.window.attributes.screenBrightness
        if (own >= 0) return (own * 100).roundToInt()

        val system = Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, SYSTEM_DEFAULT)
        return (system / 2.55f).roundToInt()
    }

    private companion object {
        //the middle of 0-255, should the system not say
        const val SYSTEM_DEFAULT = 128
    }
}

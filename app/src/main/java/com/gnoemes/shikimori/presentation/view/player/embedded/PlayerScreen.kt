package com.gnoemes.shikimori.presentation.view.player.embedded

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.view.View

/**
 * The player's screen: full screen, the rotation button and the lock's hold on the orientation, and
 * picture-in-picture.
 */
class PlayerScreen(private val activity: Activity) {

    val hasPip: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private val isAutoRotationEnabled: Boolean
        get() = Settings.System.getInt(activity.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

    //what the screen is in now. It used to be compared with the constants for requesting one, which
    //matched only by coincidence (BUGS #26)
    private val isPortrait: Boolean
        get() = activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    fun enterPip() {
        if (!hasPip) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) activity.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) activity.enterPictureInPictureMode()
    }

    fun hideSystemUi() {
        activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    /** The rotation button: the other orientation. */
    fun toggleOrientation() {
        activity.requestedOrientation =
                if (isPortrait) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    /** Locked, the screen stays as it is; unlocked, auto-rotation takes over again when it is on. */
    fun holdOrientation(locked: Boolean) {
        activity.requestedOrientation =
                if (isPortrait) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        if (!locked && isAutoRotationEnabled) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    /** After a turn: with auto-rotation on, the sensor decides again. */
    fun onConfigurationChanged() {
        if (isAutoRotationEnabled) activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}

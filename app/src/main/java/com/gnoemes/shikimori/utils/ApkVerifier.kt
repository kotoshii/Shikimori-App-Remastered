package com.gnoemes.shikimori.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File

/**
 * Checks a downloaded apk against the app that is running before it is offered for install.
 *
 * The download comes from this fork's own github over https, which is not a reason to skip this:
 * the file lands in shared app storage where other software on the device can reach it, and a
 * wrong or tampered apk installed over the user's app is the worst thing this feature could do.
 *
 * Android would refuse a mismatched signature itself - `INSTALL_FAILED_UPDATE_INCOMPATIBLE` - but
 * only after the installer has been opened, which tells the user nothing useful. Checking here
 * means a bad file is deleted and explained instead.
 *
 * **A debug build can never pass this**, because it is signed with the debug key while the release
 * on github is signed with the release key. That is correct behaviour, not a bug: testing the
 * install path needs a signed release build.
 */
object ApkVerifier {

    private const val TAG = "AppUpdate"

    /**
     * True only when [apk] is the same app, signed with the same certificate, as the one running.
     *
     * `GET_SIGNATURES` is deprecated in favour of `GET_SIGNING_CERTIFICATES` on api 28, but it
     * still works at `targetSdk 28` and is the only form that covers `minSdk 16` in one path.
     */
    @Suppress("DEPRECATION")
    fun matchesInstalledApp(context: Context, apk: File): Boolean = try {
        val manager = context.packageManager
        val downloaded = manager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)

        when {
            downloaded == null -> {
                //not a readable apk at all - a truncated download or an html error page
                Log.e(TAG, "could not read ${apk.absolutePath} as an apk")
                false
            }
            downloaded.packageName != context.packageName -> {
                Log.e(TAG, "apk is a different app: ${downloaded.packageName}")
                false
            }
            else -> {
                val installed = manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                val ours = installed.signatures?.toHashSet().orEmpty()
                val theirs = downloaded.signatures?.toHashSet().orEmpty()

                //an apk with no signature at all must never pass, so an empty set is a mismatch
                val same = ours.isNotEmpty() && ours == theirs
                if (!same) Log.e(TAG, "signature mismatch: ${theirs.size} signature(s) in the apk")
                else Log.d(TAG, "signature matches, version code ${downloaded.versionCode}")

                same
            }
        }
    } catch (e: Exception) {
        //a failed check is a failed verification - never fall through to "install anyway"
        Log.e(TAG, "verification threw for ${apk.absolutePath}", e)
        false
    }

    /**
     * True when [apk] has a lower `versionCode` than the app already installed, which android
     * refuses to install - it reports it as an invalid package, which explains nothing.
     *
     * The version *name* is what the update check compares, and the two can disagree: the version
     * code is bumped by `AUTO_INCREMENT_ONE_STEP` on every release build, so a build made later
     * always has a higher code even if its name is lower. That is what a locally built test apk
     * runs into, and it is worth naming rather than letting the installer fail.
     */
    @Suppress("DEPRECATION")
    fun isDowngrade(context: Context, apk: File): Boolean = try {
        val manager = context.packageManager
        val downloaded = manager.getPackageArchiveInfo(apk.absolutePath, 0)
        val installed = manager.getPackageInfo(context.packageName, 0)

        //an unreadable apk is not a downgrade - matchesInstalledApp has already rejected it
        val downgrade = downloaded != null && downloaded.versionCode < installed.versionCode
        if (downgrade) {
            Log.e(TAG, "downloaded version code ${downloaded!!.versionCode} is older than " +
                    "the installed ${installed.versionCode}")
        }

        downgrade
    } catch (e: Exception) {
        //this check only adds a clearer message; a failure here must not block a valid install
        Log.e(TAG, "version check threw for ${apk.absolutePath}", e)
        false
    }
}

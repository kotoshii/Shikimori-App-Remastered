package com.gnoemes.shikimori.data.repository.app

import com.gnoemes.shikimori.entity.app.domain.AppUpdate
import io.reactivex.Completable

/**
 * The update check and what it found. The result is kept, so the changelog dialog and the download
 * work without asking github again - the settings badge can be tapped hours later, possibly offline.
 */
interface AppUpdateSource {

    /** Asks github for the newest release and keeps it; does nothing when the check is off in settings. */
    fun check(): Completable

    fun isUpdateAvailable(): Boolean

    /** The release the last check found, or null when it found none. */
    fun getAvailableUpdate(): AppUpdate?

    /**
     * Posts the update notification again when this process's check posted it while notifications were
     * off - as on Android 13+ while it asks for the permission - and they are on now. Called whenever the
     * main screen resumes, which it does when the permission dialog closes. Does nothing otherwise, so a
     * notification the user swiped away stays gone.
     */
    fun notifyIfMissed()
}

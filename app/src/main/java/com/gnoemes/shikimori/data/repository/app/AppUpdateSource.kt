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
}

package com.gnoemes.shikimori.data.repository.app.impl

import android.content.Context
import android.content.SharedPreferences
import com.gnoemes.shikimori.BuildConfig
import com.gnoemes.shikimori.data.local.services.impl.AppUpdateService
import com.gnoemes.shikimori.data.network.GithubApi
import com.gnoemes.shikimori.data.repository.app.AppUpdateSource
import com.gnoemes.shikimori.di.app.annotations.SettingsQualifier
import com.gnoemes.shikimori.entity.app.data.GithubReleaseResponse
import com.gnoemes.shikimori.entity.app.domain.AppUpdate
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.utils.compareVersions
import com.gnoemes.shikimori.utils.putBoolean
import com.gnoemes.shikimori.utils.putString
import com.gnoemes.shikimori.utils.remove
import io.reactivex.Completable
import javax.inject.Inject

/**
 * The old check compared the version name against `lastVersion` in the upstream firestore, which
 * this fork does not control, so it always reported an update. This asks our own releases instead,
 * and compares properly so only a genuinely newer tag counts.
 */
class AppUpdateSourceImpl @Inject constructor(
        private val context: Context,
        private val api: GithubApi,
        @SettingsQualifier private val prefs: SharedPreferences
) : AppUpdateSource {

    //the check can be turned off in settings; the badge and anything already stored stay as they
    //are, so an update found earlier is still reachable
    override fun check(): Completable = Completable.defer {
        if (!prefs.getBoolean(SettingsExtras.CHECK_UPDATES_ON_START, true)) Completable.complete()
        else api.getLatestRelease().doOnSuccess { store(it) }.ignoreElement()
    }

    override fun isUpdateAvailable(): Boolean = prefs.getBoolean(SettingsExtras.NEW_VERSION_AVAILABLE, false)

    override fun getAvailableUpdate(): AppUpdate? {
        val version = prefs.getString(SettingsExtras.NEW_VERSION_TAG, null)

        return if (version.isNullOrBlank()) null
        else AppUpdate(version,
                prefs.getString(SettingsExtras.NEW_VERSION_CHANGELOG, null),
                prefs.getString(SettingsExtras.NEW_VERSION_APK_URL, null))
    }

    private fun store(release: GithubReleaseResponse) {
        val hasUpdate = compareVersions(release.tag, BuildConfig.VERSION_NAME) > 0

        prefs.putBoolean(SettingsExtras.NEW_VERSION_AVAILABLE, hasUpdate)

        if (!hasUpdate) {
            //nothing newer - drop what an earlier check left, or the badge would open a stale changelog
            prefs.remove(SettingsExtras.NEW_VERSION_TAG)
            prefs.remove(SettingsExtras.NEW_VERSION_CHANGELOG)
            prefs.remove(SettingsExtras.NEW_VERSION_APK_URL)
            return
        }

        //tags are written as v0.8.10; the app talks about versions, not tags
        val version = release.tag.orEmpty().trimStart('v', 'V')

        prefs.putString(SettingsExtras.NEW_VERSION_TAG, version)
        //localizedBody, not body: the notes carry english after a marker and the dialog shows only
        //the russian half of them
        prefs.putString(SettingsExtras.NEW_VERSION_CHANGELOG, release.localizedBody)
        prefs.putString(SettingsExtras.NEW_VERSION_APK_URL, release.apkUrl)

        //this runs on an io thread and can outlive any screen, so the notification uses the app
        AppUpdateService.notifyAvailable(context, version, release.apkUrl)
    }
}

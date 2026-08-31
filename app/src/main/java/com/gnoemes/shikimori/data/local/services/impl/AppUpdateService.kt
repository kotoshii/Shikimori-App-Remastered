package com.gnoemes.shikimori.data.local.services.impl

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.presentation.view.main.MainActivity
import com.gnoemes.shikimori.presentation.view.update.InstallActivity
import com.gnoemes.shikimori.utils.ApkVerifier
import com.gnoemes.shikimori.utils.notificationManager
import java.io.File
import java.util.concurrent.Executors

/**
 * Downloads a new release of the app and offers it for install.
 *
 * The transfer itself is [VideoFileDownloader], the same one episodes use - an apk is a plain file,
 * so none of the playlist handling comes into play, but the timeouts, the partial-file cleanup and
 * the progress reporting are all worth reusing.
 *
 * The apk goes to the app's own external files directory rather than the user's download folder: no
 * storage permission is needed for it, the existing `FileProvider` already covers the path, and it
 * is removed when the app is uninstalled instead of leaving a stale installer behind.
 */
class AppUpdateService : Service() {

    companion object {
        private const val TAG = "AppUpdate"

        private const val CHANNEL_ID = "SHIKIMORI_UPDATES_CHANNEL"

        //one id per purpose: the "available" notice, the running download, and the result
        private const val AVAILABLE_ID = 4220
        private const val PROGRESS_ID = 4221
        private const val RESULT_ID = 4222

        private const val EXTRA_URL = "update_url"
        private const val EXTRA_VERSION = "update_version"

        private const val UPDATES_DIR = "updates"

        /**
         * Tells the user a new version exists. Swiping it away is allowed on purpose - the badge in
         * settings is the way back to it, which is why nothing here is ongoing.
         */
        fun notifyAvailable(context: Context, version: String, apkUrl: String?) {
            createChannel(context)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle(context.getString(R.string.update_available_title, version))
                    .setContentText(context.getString(R.string.update_available_text))
                    .setSmallIcon(R.drawable.ic_notification_download)
                    .setAutoCancel(true)
                    .setContentIntent(changelogIntent(context))
                    .addAction(R.drawable.ic_download,
                            context.getString(R.string.update_action_changes), changelogIntent(context))

            //no download action without an apk to download - a release published without one is
            //still worth announcing, it just has to be installed by hand
            if (!apkUrl.isNullOrBlank()) {
                builder.addAction(R.drawable.ic_download,
                        context.getString(R.string.update_action_download),
                        downloadIntent(context, version, apkUrl))
            } else {
                Log.w(TAG, "release $version has no apk asset")
            }

            context.notificationManager().notify(AVAILABLE_ID, builder.build())
        }

        /** Starts the download. Used by the notification action and by the changelog dialog. */
        fun download(context: Context, version: String, apkUrl: String) {
            val intent = Intent(context, AppUpdateService::class.java)
                    .putExtra(EXTRA_VERSION, version)
                    .putExtra(EXTRA_URL, apkUrl)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        private fun downloadIntent(context: Context, version: String, apkUrl: String): PendingIntent {
            val intent = Intent(context, AppUpdateService::class.java)
                    .putExtra(EXTRA_VERSION, version)
                    .putExtra(EXTRA_URL, apkUrl)

            //a foreground service started from a notification action needs its own factory method
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                PendingIntent.getForegroundService(context, 1, intent, pendingIntentFlags())
            else PendingIntent.getService(context, 1, intent, pendingIntentFlags())
        }

        private fun changelogIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_SHOW_CHANGELOG, true)
                    //reuses the running activity so the dialog appears over the app the user has
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            return PendingIntent.getActivity(context, 2, intent, pendingIntentFlags())
        }

        /** Matches `VideoDownloadService`: immutable is only required from Android 12, but is free here. */
        private fun pendingIntentFlags(): Int =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                else PendingIntent.FLAG_UPDATE_CURRENT

        private fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            //separate from the episode downloads channel: a user who mutes one rarely means both
            val channel = NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.update_notification_channel),
                    NotificationManager.IMPORTANCE_LOW)
            context.notificationManager().createNotificationChannel(channel)
        }
    }

    private val executor = Executors.newSingleThreadExecutor()

    //a second tap on "Загрузить" would otherwise queue another run, and the first thing a run does
    //is clear the directory - which would delete the apk the finished notification points at
    @Volatile
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        val version = intent?.getStringExtra(EXTRA_VERSION).orEmpty()

        //startForegroundService demands this within a few seconds, before the extras are trusted
        startForeground(PROGRESS_ID, progressNotification(version, 0))

        if (url.isNullOrBlank()) {
            Log.e(TAG, "no url to download")
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        if (running) {
            Log.d(TAG, "already downloading, ignoring the repeated request")
            return START_NOT_STICKY
        }

        //the announcement has been acted on, so it should not sit there through the download
        notificationManager().cancel(AVAILABLE_ID)

        running = true
        executor.execute { run(version, url) }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun run(version: String, url: String) {
        var target: File? = null

        try {
            val directory = updatesDirectory()
            //only one update is ever wanted; an abandoned apk from a previous version is clutter
            directory.listFiles()?.forEach { it.delete() }

            val apk = File(directory, "ShikimoriApp-$version.apk")
            target = apk

            Log.d(TAG, "downloading $url -> ${apk.absolutePath}")
            val downloaded = VideoFileDownloader()
                    .download(url, emptyMap(), apk, progress(version))

            when {
                !downloaded -> notifyResult(getString(R.string.update_download_failed), version, null)
                !ApkVerifier.matchesInstalledApp(this, apk) -> {
                    //a file that fails the check is not kept - it cannot be used for anything
                    apk.delete()
                    notifyResult(getString(R.string.update_signature_mismatch), version, null)
                }
                ApkVerifier.isDowngrade(this, apk) -> {
                    //android would refuse this install and call the package invalid, which says
                    //nothing about what is actually wrong
                    apk.delete()
                    notifyResult(getString(R.string.update_older_build), version, null)
                }
                else -> {
                    Log.d(TAG, "ready to install: ${apk.absolutePath}")
                    notifyResult(getString(R.string.update_downloaded_title), version, apk)
                }
            }
        } catch (e: Throwable) {
            //never let the worker thread die silently - the user would be left with a stuck bar
            Log.e(TAG, "update download threw", e)
            target?.delete()
            notifyResult(getString(R.string.update_download_failed), version, null)
        } finally {
            running = false
            stopForeground(true)
            stopSelf()
        }
    }

    /**
     * External storage, not internal: the pre-Nougat installer reads the apk through a `file://`
     * uri and cannot see the app's private internal directory. Falling back to internal storage
     * keeps the download working everywhere else rather than failing outright.
     */
    private fun updatesDirectory(): File {
        val external = getExternalFilesDir(null)
        if (external == null) Log.w(TAG, "no external files dir, falling back to internal storage")

        return File(external ?: filesDir, UPDATES_DIR).apply { mkdirs() }
    }

    private fun progress(version: String) = object : VideoFileDownloader.Progress {
        private var last = -1

        override fun onProgress(percent: Int): Boolean {
            if (percent < 0 || percent == last) return true

            last = percent
            notificationManager().notify(PROGRESS_ID, progressNotification(version, percent))
            return true
        }
    }

    private fun progressNotification(version: String, percent: Int) =
            NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.update_available_title, version))
                    .setContentText(getString(R.string.update_downloading))
                    .setSmallIcon(R.drawable.ic_notification_download)
                    .setProgress(100, percent, false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()

    /** [apk] is null when there is nothing to install - the text then says why. */
    private fun notifyResult(title: String, version: String, apk: File?) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.ic_notification_download)
                .setAutoCancel(true)

        if (apk != null) {
            val install = InstallActivity.intent(this, apk)
            val pending = PendingIntent.getActivity(this, 3, install, pendingIntentFlags())

            builder.setContentText(getString(R.string.update_downloaded_text, version))
                    .setContentIntent(pending)
                    .addAction(R.drawable.ic_download, getString(R.string.update_action_install), pending)
        }

        //a separate id, so the result survives the foreground notification going away
        notificationManager().notify(RESULT_ID, builder.build())
    }
}

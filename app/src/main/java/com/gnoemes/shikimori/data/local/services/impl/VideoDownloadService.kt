package com.gnoemes.shikimori.data.local.services.impl

import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.download.DownloadVideoData
import com.gnoemes.shikimori.utils.VideoMuxer
import com.gnoemes.shikimori.utils.notificationManager
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Downloads an episode in the app's own process.
 *
 * Replaces Android's `DownloadManager`, which could only copy one url to one file. That broke two
 * things at once: an m3u8 was saved as the playlist rather than the video, and the transfer happened
 * in `com.android.providers.downloads`, a separate process that is not necessarily on the same
 * network the app is (a per-app vpn will leave it stranded, reported as "waiting for connection").
 *
 * Runs one download at a time and stops itself when the queue drains.
 */
class VideoDownloadService : Service() {

    companion object {
        private const val TAG = "VideoDownload"

        private const val CHANNEL_ID = "SHIKIMORI_DOWNLOADS_CHANNEL"
        private const val FOREGROUND_ID = 4210

        private const val EXTRA_DATA = "download_data"
        private const val EXTRA_FOLDER = "download_folder"
        private const val EXTRA_CANCEL = "download_cancel"

        private const val VIDEO_SUFFIX = "(video)"
        private const val AUDIO_SUFFIX = "(audio)"

        //hls segments arrive as mpeg-ts and are only remuxed to mp4 afterwards
        private const val STREAM_EXTENSION = ".ts"

        private val ILLEGAL_NAME_CHARS = Regex("[\\\\/:*?\"<>|]|\\p{Cntrl}")
        private val REPEATED_SPACES = Regex("\\s+")

        /**
         * ext4 - which backs Android's shared storage - caps a single path component at **255
         * bytes**, and going over fails the file with ENAMETOOLONG, which surfaces to the user as
         * an unexplained failed download. Cyrillic costs two bytes a character, so this is reached
         * sooner than it looks: 255 bytes is only about 127 letters.
         *
         * 240 leaves room for the longest suffix appended anywhere here, " (video).mp4" at 12 bytes.
         */
        private const val MAX_NAME_BYTES = 240

        fun enqueue(context: Context, data: DownloadVideoData, folder: String) {
            val intent = Intent(context, VideoDownloadService::class.java)
                    .putExtra(EXTRA_DATA, data)
                    .putExtra(EXTRA_FOLDER, folder)

            //a foreground service must be started as one from Android 8, or it is killed on start
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }

    //recreated after a cancel: shutdownNow() kills it for good, and stopSelf() does not destroy the
    //service straight away, so a download started right after a cancel would otherwise arrive at a
    //dead executor and throw RejectedExecutionException on the main thread
    private var executor = Executors.newSingleThreadExecutor()
    private val pending = AtomicInteger(0)

    @Volatile
    private var cancelled = false

    /** What the ongoing notification should be labelled with, i.e. the download actually running. */
    @Volatile
    private var currentTitle: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val data = intent?.getParcelableExtra<DownloadVideoData>(EXTRA_DATA)
        val folder = intent?.getStringExtra(EXTRA_FOLDER)
        val isCancel = intent?.getBooleanExtra(EXTRA_CANCEL, false) == true

        if (!isCancel && data != null && folder != null) {
            //a new download undoes a previous cancel, on both the flag and the executor
            cancelled = false
            if (executor.isShutdown) executor = Executors.newSingleThreadExecutor()

            //counted before the notification is built, so a newly queued download is included in it
            pending.incrementAndGet()
        }

        //startForegroundService demands a startForeground within a few seconds even when there is
        //nothing to do, so this happens before the extras are trusted. The title stays on whatever
        //is actually downloading - a second tap must not relabel it with the one it just queued.
        startForeground(FOREGROUND_ID,
                notification(currentTitle ?: data?.let(::title).orEmpty(), 0, ongoing = true))

        if (isCancel) {
            Log.d(TAG, "cancelled by the user")
            cancelled = true
            //interrupts the transfer; VideoFileDownloader deletes the partial file on the way out
            executor.shutdownNow()
            notificationManager().notify(FOREGROUND_ID + 1, finishedNotification(
                    getString(R.string.download_notification_cancelled), ""))
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        if (data == null || folder == null) {
            if (pending.get() == 0) {
                stopForeground(true)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        executor.execute { runDownload(data, folder) }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        cancelled = true
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun runDownload(data: DownloadVideoData, folder: String) {
        val title = title(data)
        val directory = File(File(folder), "anime/" + truncateToBytes(safeName(data.animeName), MAX_NAME_BYTES))
        val downloader = VideoFileDownloader()
        val link = data.link

        currentTitle = title

        Log.d(TAG, "start: link=$link audio=${data.audioLink} dir=${directory.absolutePath}")
        Log.d(TAG, "dir exists=${directory.exists()} canWrite=${directory.canWrite()} " +
                "parentExists=${directory.parentFile?.exists()} folderSetting=$folder")

        var result: Result? = null
        try {
            if (link == null) Log.e(TAG, "no link in DownloadVideoData - nothing to download")
            else result = download(downloader, data, link, title, directory)
        } catch (e: Throwable) {
            //never let the worker thread die silently - the notification alone says too little
            Log.e(TAG, "download threw", e)
        } finally {
            Log.d(TAG, "finished success=${result != null} merged=${result?.merged} " +
                    "file=${result?.file?.absolutePath}")
            //a cancel is not a failure, and the user already got its own notification
            if (!cancelled) notifyFinished(title, result)
            currentTitle = null
            if (pending.decrementAndGet() == 0) {
                stopForeground(true)
                stopSelf()
            }
        }
    }

    /**
     * What a finished download produced. [merged] is false only when a hosting served sound
     * separately and the two files could not be joined - the video plays, but silently, so the user
     * has to be told rather than handed a file that looks complete.
     */
    private class Result(val file: File, val merged: Boolean = true)

    /** The finished download, or null if it failed. */
    private fun download(downloader: VideoFileDownloader, data: DownloadVideoData,
                         link: String, title: String, directory: File): Result? {
        val audioLink = data.audioLink
        val output = File(directory, "$title.mp4")

        //hostings that serve sound separately are fetched as two parts and joined, as before
        if (!audioLink.isNullOrBlank()) {
            val video = File(directory, "$title $VIDEO_SUFFIX.mp4")
            val audio = File(directory, "$title $AUDIO_SUFFIX.m4a")

            //discarded rather than left behind: a lone video part is not a download, and the
            //notification is about to say the download failed
            if (!downloader.download(link, data.requestHeaders, video, progress(title, 0, 45))) {
                return discard(video, audio)
            }
            if (!downloader.download(audioLink, data.requestHeaders, audio, progress(title, 45, 90))) {
                return discard(video, audio)
            }

            val muxed = VideoMuxer.isSupported &&
                    VideoMuxer.mux(video.absolutePath, audio.absolutePath, output.absolutePath,
                            muxProgress(title, R.string.download_notification_merging))
            //a cancel during muxing leaves both parts and a partial output; none of it is wanted
            if (cancelled) return discard(video, audio, output)

            if (muxed) {
                video.delete()
                audio.delete()
                return Result(output)
            }

            //Both parts stay when muxing is unavailable (it needs api 18, minSdk is 16) or failed.
            //The video alone has no sound, so this is reported as a partial result - saying
            //"finished" and then playing a silent file would be a lie.
            return video.takeIf { it.exists() }?.let { Result(it, merged = false) }
        }

        //a playlist is downloaded as mpeg-ts first, because that is what the segments are
        val isStream = link.contains(".m3u8", ignoreCase = true)
        val target = if (isStream) File(directory, title + STREAM_EXTENSION) else output

        if (!downloader.download(link, data.requestHeaders, target, progress(title, 0, 100))) {
            return discard(target)
        }
        if (!isStream) return Result(target)

        //mp4 is what a user expects; keep the .ts if the device cannot remux
        val remuxed = VideoMuxer.isSupported &&
                VideoMuxer.remux(target.absolutePath, output.absolutePath,
                        muxProgress(title, R.string.download_notification_converting))
        //a cancel during remuxing leaves the .ts and a partial mp4
        if (cancelled) return discard(target, output)

        if (remuxed) {
            target.delete()
            return Result(output)
        }

        //keeping the .ts is better than losing the download, and it plays with sound
        return Result(target)
    }

    /**
     * Deletes whatever a failed or cancelled download left behind and reports failure. Downloads
     * are not resumable, so a half finished set of files is only clutter - and after a cancel the
     * user expects it gone.
     */
    private fun discard(vararg files: File): Result? {
        files.forEach { file ->
            if (file.exists() && !file.delete()) Log.e(TAG, "could not delete ${file.absolutePath}")
        }
        return null
    }

    private fun progress(title: String, from: Int, to: Int) = object : VideoFileDownloader.Progress {
        private var last = -1

        override fun onProgress(percent: Int): Boolean {
            if (cancelled) return false
            if (percent < 0) return true

            val scaled = from + percent * (to - from) / 100
            //notifications are rate limited by the system, so only redraw on a real change
            if (scaled != last) {
                last = scaled
                notificationManager().notify(FOREGROUND_ID, notification(title, scaled, ongoing = true))
            }
            return true
        }
    }

    /**
     * Muxing a full episode is not instant, so the bar keeps moving instead of freezing at 100%.
     * `VideoMuxer` reports against the track duration, which is the only measure it has.
     *
     * The two paths are described differently on purpose: joining a separate audio file really is
     * "объединение видео и звука", but rewriting one mpeg-ts into mp4 combines nothing at all.
     */
    private fun muxProgress(title: String, stageRes: Int): (Int) -> Boolean {
        var last = -1
        val stage = getString(stageRes)

        return { percent ->
            if (percent != last) {
                last = percent
                notificationManager().notify(FOREGROUND_ID, notification(title, percent, true, stage))
            }
            //MediaMuxer ignores thread interrupts, so returning false here is the only way a cancel
            //can reach a mux that is already running
            !cancelled
        }
    }

    /**
     * Two downloads of the same episode used to collide: the name held only the episode number and
     * the anime title, so pulling a dub from one hosting silently overwrote a sub already saved from
     * another. Author, kind and hosting are appended to tell them apart.
     */
    /**
     * `[author] Anime name - 07 (kind, hosting)`.
     *
     * Square brackets say who made it, round ones say what it is, so both scan at a glance. An
     * unknown author is left out entirely rather than shown as empty brackets.
     */
    private fun title(data: DownloadVideoData): String {
        //padded so episode 10 does not sort before episode 2 in a file manager
        val episode = data.episodeIndex.toString().padStart(2, '0')
        val meta = listOf(data.quality, data.kind, data.hosting).filter { it.isNotBlank() }

        val prefix = if (data.author.isBlank()) "" else "[" + safeName(data.author) + "] "
        val suffix = " - " + episode + if (meta.isEmpty()) "" else " (" + meta.joinToString(", ") + ")"

        //Only the anime name is ever shortened. Cutting the tail instead would eat the metadata -
        //which is the whole reason two downloads of one episode stay apart - and could leave a
        //half-written "(озвуч". The name is also repeated in the folder, so it loses the least.
        val room = MAX_NAME_BYTES - prefix.toByteArray().size - suffix.toByteArray().size
        val animeName = truncateToBytes(safeName(data.animeName), room)

        //backstop for a pathological author or hosting; a no-op in every normal case
        return truncateToBytes(prefix + animeName + suffix, MAX_NAME_BYTES)
    }

    /**
     * Android's storage layer refuses to create a *file* whose name holds any of these. A colon is
     * what actually bit us - "Re:Zero" produced a directory that could be created and then every
     * file inside it failed with EPERM, which reads like a permissions problem and is not one.
     * Directories tolerate more than files do, so both go through here.
     */
    private fun safeName(name: String): String = name
            .replace(ILLEGAL_NAME_CHARS, " ")
            .replace(REPEATED_SPACES, " ")
            .trim()
            .trimEnd('.')

    /** The filesystem limit is in **bytes**, and cyrillic costs two each, so characters cannot be counted. */
    private fun truncateToBytes(value: String, maxBytes: Int): String {
        if (maxBytes <= 0) return ""

        var result = value
        while (result.toByteArray().size > maxBytes) result = result.dropLast(1)

        return result.trim()
    }

    private fun notification(title: String, percent: Int, ongoing: Boolean,
                             stage: String? = null): android.app.Notification {
        //downloads run one at a time on purpose - two large files would only halve each other's
        //bandwidth - so say how many are waiting rather than let a queued one look ignored
        val waiting = pending.get() - 1
        val text = stage
                ?: if (waiting > 0) getString(R.string.download_notification_queued, waiting)
                else getString(R.string.download_notification_progress)

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification_download)
                .setProgress(100, percent, false)
                .setOngoing(ongoing)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.ic_download, getString(R.string.download_notification_cancel), cancelIntent())
                .build()
    }

    /** Stops the running download and drops anything still queued behind it. */
    private fun cancelIntent(): PendingIntent {
        val intent = Intent(this, VideoDownloadService::class.java).putExtra(EXTRA_CANCEL, true)

        return PendingIntent.getService(this, 0, intent, pendingIntentFlags())
    }

    /**
     * FLAG_IMMUTABLE is only required from Android 12 for apps targeting it, but it costs nothing
     * here and keeps these correct if the target is ever raised.
     */
    private fun pendingIntentFlags(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT

    private fun finishedNotification(title: String, text: String) =
            NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_notification_download)
                    .setAutoCancel(true)
                    .build()

    private fun notifyFinished(title: String, result: Result?) {
        val text = when {
            result == null -> getString(R.string.download_notification_failed)
            !result.merged -> getString(R.string.download_notification_no_audio)
            else -> getString(R.string.download_notification_done)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification_download)
                .setAutoCancel(true)

        if (result != null) openIntent(result.file)?.let(builder::setContentIntent)

        //a separate id, so the result survives the foreground notification going away
        notificationManager().notify(FOREGROUND_ID + 1 + title.hashCode().and(0xFFF), builder.build())
    }

    /**
     * Opens the downloaded file itself. There is deliberately no "open the containing folder" here:
     * Android has no reliable intent for it and the result varies by device and file manager, while
     * playing the file works everywhere.
     *
     * The uri has to come from `FileProvider` - handing another app a `file://` uri throws
     * `FileUriExposedException` on Android 7+. The provider is already declared in the manifest and
     * its `external-path` covers the download folder, so nothing else was needed.
     */
    private fun openIntent(file: File): PendingIntent? = try {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
                //a .ts is what is left when the device could not remux, and it still plays
                .setDataAndType(uri, if (file.extension.equals("ts", true)) "video/mp2t" else "video/mp4")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

        PendingIntent.getActivity(this, file.hashCode(), intent, pendingIntentFlags())
    } catch (e: Exception) {
        //an unshareable path must not cost the user their "download finished" notification
        Log.e(TAG, "cannot build an open intent for ${file.absolutePath}", e)
        null
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.download_notification_channel),
                NotificationManager.IMPORTANCE_LOW)
        notificationManager().createNotificationChannel(channel)
    }
}

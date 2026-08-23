package com.gnoemes.shikimori.data.local.services.impl

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.services.DownloadSource
import com.gnoemes.shikimori.entity.download.DownloadVideoData
import com.gnoemes.shikimori.entity.download.PendingMux
import com.gnoemes.shikimori.utils.downloadManager
import com.gnoemes.shikimori.utils.toUri
import io.reactivex.Completable
import java.io.File
import javax.inject.Inject

class DownloadManagerSourceImpl @Inject constructor(
        private val context: Context,
        private val settingsSource: SettingsSource
) : DownloadSource {

    override fun downloadVideo(data: DownloadVideoData): Completable {
        return if (data.link.isNullOrBlank()) Completable.error(NoSuchElementException())
        else Completable.fromAction {
            val title = String.format(context.getString(R.string.episode_number), data.episodeIndex).plus(" ${data.animeName}")

            val folder = File(settingsSource.downloadFolder)
            val manager = context.downloadManager() ?: return@fromAction
            val audioLink = data.audioLink

            if (audioLink.isNullOrBlank()) {
                manager.enqueue(request(data, title, "$title.mp4", data.link, folder))
                return@fromAction
            }

            //hostings that keep the sound in its own file are downloaded as two parts under
            //temporary names, VideoMuxService joins them once both are on disk
            val videoId = manager.enqueue(request(data, title, "$title $VIDEO_SUFFIX.mp4", data.link, folder))
            val audioId = manager.enqueue(request(data, "$title $AUDIO_SUFFIX", "$title $AUDIO_SUFFIX.m4a", audioLink, folder))

            val output = File(folder, "anime/${data.animeName}/$title.mp4").absolutePath
            PendingMuxStore(context).add(PendingMux(videoId, audioId, output))
        }
    }

    private fun request(data: DownloadVideoData, title: String, fileName: String, link: String, folder: File) =
            DownloadManager.Request(link.toUri())
                    .setTitle(title)
                    .setDescription(context.getString(R.string.app_name))
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .apply {
                        allowScanningByMediaScanner()
                        setDestinationUri(Uri.withAppendedPath(Uri.fromFile(folder), "anime/${data.animeName}/$fileName"))
                        data.requestHeaders.entries.forEach { addRequestHeader(it.key, it.value) }
                    }

    companion object {
        private const val VIDEO_SUFFIX = "(video)"
        private const val AUDIO_SUFFIX = "(audio)"
    }
}
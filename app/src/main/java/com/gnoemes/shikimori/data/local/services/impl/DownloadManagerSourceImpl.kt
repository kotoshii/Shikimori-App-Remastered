package com.gnoemes.shikimori.data.local.services.impl

import android.content.Context
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.services.DownloadSource
import com.gnoemes.shikimori.entity.download.DownloadVideoData
import io.reactivex.Completable
import javax.inject.Inject

/**
 * Hands a download to [VideoDownloadService].
 *
 * This used to enqueue against Android's `DownloadManager`, which could only copy one url to one
 * file. That saved the *playlist* for every hls hosting - Kodik included, which is the primary
 * source - and ran the transfer in a separate system process that is not always on the same network
 * as the app. Doing it in-process fixes both.
 */
class DownloadManagerSourceImpl @Inject constructor(
        private val context: Context,
        private val settingsSource: SettingsSource
) : DownloadSource {

    override fun downloadVideo(data: DownloadVideoData): Completable {
        return if (data.link.isNullOrBlank()) Completable.error(NoSuchElementException())
        else Completable.fromAction {
            val folder = settingsSource.downloadFolder
            if (folder.isBlank()) throw NoSuchElementException()

            VideoDownloadService.enqueue(context, data, folder)
        }
    }
}

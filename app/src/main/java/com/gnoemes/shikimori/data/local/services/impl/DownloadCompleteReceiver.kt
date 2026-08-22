package com.gnoemes.shikimori.data.local.services.impl

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Wakes the muxer up when a download finishes, so a video and its separate audio file can be
 * joined once both are on disk.
 */
class DownloadCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, NO_ID)
        if (downloadId == NO_ID) return

        VideoMuxService.onDownloadFinished(context.applicationContext, downloadId)
    }

    companion object {
        private const val NO_ID = -1L
    }
}

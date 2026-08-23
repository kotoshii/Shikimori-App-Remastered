package com.gnoemes.shikimori.data.local.services.impl

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
import com.gnoemes.shikimori.utils.VideoMuxer
import com.gnoemes.shikimori.utils.downloadManager
import java.io.File

/**
 * Joins a finished video download with its audio download.
 *
 * Runs as a job rather than straight from the receiver because remuxing a full episode takes far
 * longer than a broadcast is allowed to live, and it has to keep working on Android 8 and up where
 * background services are not started from receivers.
 */
class VideoMuxService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, NO_ID)
        if (downloadId == NO_ID) return

        val store = PendingMuxStore(applicationContext)
        val job = store.find(downloadId) ?: return

        val manager = applicationContext.downloadManager() ?: return

        //the other half may still be downloading, this runs again when it reports in
        val videoPath = manager.finishedFile(job.videoId) ?: return
        val audioPath = manager.finishedFile(job.audioId) ?: return

        if (VideoMuxer.mux(videoPath, audioPath, job.outputPath)) {
            File(videoPath).delete()
            File(audioPath).delete()
        }

        store.remove(job)
    }

    /**
     * Local path of a download that completed successfully, or null while it is still running or
     * if it failed.
     */
    private fun DownloadManager.finishedFile(id: Long): String? {
        val cursor = query(DownloadManager.Query().setFilterById(id)) ?: return null

        return cursor.use {
            if (!it.moveToFirst()) return null

            val status = it.getInt(it.getColumnIndex(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) return null

            val uri = it.getString(it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)) ?: return null
            Uri.parse(uri).path
        }
    }

    companion object {
        private const val JOB_ID = 4210
        private const val EXTRA_DOWNLOAD_ID = "EXTRA_DOWNLOAD_ID"
        private const val NO_ID = -1L

        fun onDownloadFinished(context: Context, downloadId: Long) {
            val intent = Intent().putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            enqueueWork(context, VideoMuxService::class.java, JOB_ID, intent)
        }
    }
}

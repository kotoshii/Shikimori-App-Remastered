package com.gnoemes.shikimori.data.local.services.impl

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * Moves the bytes for one download.
 *
 * This exists because Android's `DownloadManager` can only copy a single url to a single file.
 * Given an m3u8 that saved the *playlist* - a few hundred bytes of text named `.mp4` - which is
 * what every hls hosting produced, Kodik included. Walking the playlist here fixes that, and as a
 * side effect the transfer runs in the app's own process rather than in
 * `com.android.providers.downloads`, which matters on devices where that process is not routed the
 * same way (a per-app vpn, for instance).
 *
 * Segments are appended to one file exactly as they arrive. MPEG-TS concatenates losslessly, so the
 * result is playable as-is; `VideoDownloadService` then remuxes it to mp4 when it can.
 */
class VideoFileDownloader {

    companion object {
        private const val TAG = "VideoDownload"
        private const val BUFFER_SIZE = 128 * 1024
        private const val TIMEOUT_SECONDS = 30L

        //a master playlist lists other playlists rather than segments
        private const val VARIANT_MARKER = "#EXT-X-STREAM-INF"
    }

    interface Progress {
        /** [percent] is -1 while the total size is unknown. Returns false to abort the download. */
        fun onProgress(percent: Int): Boolean
    }

    private val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    /**
     * Returns true only when [target] holds the complete download, so the caller can decide whether
     * to keep it. A partial file is deleted on failure rather than left to look finished.
     */
    fun download(url: String, headers: Map<String, String>, target: File, progress: Progress): Boolean {
        target.parentFile?.mkdirs()

        return try {
            if (isPlaylist(url)) downloadPlaylist(url, headers, target, progress)
            else downloadFile(url, headers, target, progress)
        } catch (e: Exception) {
            //anything at all means the download failed - including SecurityException, which is how
            //a refused storage path surfaces. Never let it reach the caller as a crash.
            Log.e(TAG, "failed: $url -> ${target.absolutePath}", e)
            false
        }.also { finished -> if (!finished) target.delete() }
    }

    private fun isPlaylist(url: String) = url.contains(".m3u8", ignoreCase = true)

    private fun downloadFile(url: String, headers: Map<String, String>, target: File, progress: Progress): Boolean {
        target.outputStream().use { out ->
            return copy(url, headers, out, progress, 0L, -1L)
        }
    }

    /**
     * Downloads every segment of a media playlist into a single file. A master playlist is followed
     * one level down first - its highest variant - because some hostings hand out the master url.
     */
    private fun downloadPlaylist(url: String, headers: Map<String, String>, target: File, progress: Progress): Boolean {
        val playlist = readText(url, headers)
        if (playlist == null) {
            Log.e(TAG, "playlist could not be read: $url")
            return false
        }

        if (playlist.contains(VARIANT_MARKER)) {
            val variant = entries(playlist, url).firstOrNull() ?: return false
            //only one level - a variant pointing at another master is not a real playlist
            return if (isPlaylist(variant)) downloadPlaylist(variant, headers, target, NoRecursionProgress(progress))
            else downloadFile(variant, headers, target, progress)
        }

        val segments = entries(playlist, url)
        if (segments.isEmpty()) {
            Log.e(TAG, "playlist has no segments: $url")
            return false
        }
        Log.d(TAG, "playlist has ${segments.size} segments: $url")

        target.outputStream().use { out ->
            segments.forEachIndexed { index, segment ->
                if (!progress.onProgress(index * 100 / segments.size)) return false
                if (!copy(segment, headers, out, SegmentProgress(progress), 0L, -1L)) return false
            }
        }

        progress.onProgress(100)
        return true
    }

    /** Every non-comment line of a playlist, resolved against the playlist's own url. */
    private fun entries(playlist: String, playlistUrl: String): List<String> {
        val base = HttpUrl.parse(playlistUrl) ?: return emptyList()

        return playlist.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                //resolve() handles "./x.ts", "../x.ts" and absolute urls alike
                .mapNotNull { base.resolve(it)?.toString() }
    }

    private fun readText(url: String, headers: Map<String, String>): String? {
        val response = client.newCall(request(url, headers)).execute()

        return response.use {
            if (!it.isSuccessful) null else it.body()?.string()
        }
    }

    private fun copy(url: String, headers: Map<String, String>, out: OutputStream,
                     progress: Progress, written: Long, knownTotal: Long): Boolean {
        val response = client.newCall(request(url, headers)).execute()

        return response.use {
            val body = it.body()
            if (!it.isSuccessful || body == null) {
                Log.e(TAG, "HTTP ${it.code()} for $url")
                return@use false
            }

            val total = if (knownTotal > 0) knownTotal else body.contentLength()
            val buffer = ByteArray(BUFFER_SIZE)
            var done = written

            body.byteStream().use { input ->
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break

                    out.write(buffer, 0, read)
                    done += read

                    val percent = if (total > 0) (done * 100 / total).toInt() else -1
                    if (!progress.onProgress(percent)) return@use false
                }
            }
            true
        }
    }

    private fun request(url: String, headers: Map<String, String>): Request {
        val builder = Request.Builder().url(url)
        headers.forEach { (name, value) -> builder.addHeader(name, value) }

        return builder.build()
    }

    /**
     * A segment does not report its own share of the total - progress is counted in whole segments -
     * but a cancel still has to be noticed inside one, or stopping a download would wait for the
     * current segment to finish.
     */
    private class SegmentProgress(private val delegate: Progress) : Progress {
        override fun onProgress(percent: Int) = delegate.onProgress(-1)
    }

    /** Keeps cancellation working through the one allowed master-playlist hop. */
    private class NoRecursionProgress(private val delegate: Progress) : Progress {
        override fun onProgress(percent: Int) = delegate.onProgress(percent)
    }
}

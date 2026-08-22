package com.gnoemes.shikimori.data.local.services.impl

import android.content.Context
import com.gnoemes.shikimori.entity.download.PendingMux
import com.gnoemes.shikimori.utils.getDefaultSharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Survives the app being closed, since the download manager keeps working without it.
 */
class PendingMuxStore(private val context: Context) {

    private val gson = Gson()

    @Synchronized
    fun add(job: PendingMux) = save(all().plus(job))

    @Synchronized
    fun remove(job: PendingMux) = save(all().filterNot { it.videoId == job.videoId })

    @Synchronized
    fun find(downloadId: Long): PendingMux? = all().firstOrNull { it.owns(downloadId) }

    private fun all(): List<PendingMux> {
        val raw = context.getDefaultSharedPreferences().getString(KEY, null) ?: return emptyList()
        return try {
            gson.fromJson<List<PendingMux>>(raw, object : TypeToken<List<PendingMux>>() {}.type) ?: emptyList()
        } catch (e: Throwable) {
            emptyList()
        }
    }

    private fun save(jobs: List<PendingMux>) {
        context.getDefaultSharedPreferences()
                .edit()
                .putString(KEY, gson.toJson(jobs))
                .apply()
    }

    companion object {
        private const val KEY = "PENDING_MUX_JOBS"
    }
}

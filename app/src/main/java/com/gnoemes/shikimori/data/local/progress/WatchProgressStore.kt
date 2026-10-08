package com.gnoemes.shikimori.data.local.progress

import com.gnoemes.shikimori.entity.series.domain.WatchProgress
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The last watched count the app read from or wrote to shikimori, per anime, held in memory only.
 *
 * The series screen fills it with the count it was opened with; every read and write in
 * `WatchProgressInteractor` updates it. That is how the episode list and the player - a separate
 * screen - see the count the others left, with no request of their own. It replaces the
 * `episodes` and `anime_rate_sync` tables (database version 5 drops them).
 */
@Singleton
class WatchProgressStore @Inject constructor() {

    private val progress = ConcurrentHashMap<Long, WatchProgress>()

    fun get(animeId: Long): WatchProgress? = progress[animeId]

    fun put(animeId: Long, value: WatchProgress) {
        progress[animeId] = value
    }

    /** 0 when the anime is not in the list, or nothing is known about it yet. */
    fun watchedEpisodes(animeId: Long): Int = progress[animeId]?.episodes ?: 0
}

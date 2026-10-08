package com.gnoemes.shikimori.domain.series

import io.reactivex.Completable

/**
 * The watched count of an anime, read from and written to shikimori - the app keeps no episode
 * state of its own. Episode N counts as watched when N is not above the count.
 */
interface WatchProgressInteractor {

    /** What the series screen was opened with, so the episode list and the player start from it. */
    fun remember(animeId: Long, rateId: Long?, watchedEpisodes: Int)

    /** The count as last read or written; 0 when the anime is not in the list or not known yet. */
    fun watchedEpisodes(animeId: Long): Int

    /**
     * «Автоматический подсчет»: adds 1 when [episode] is the next one. A later or an earlier
     * episode changes nothing - opening one to check it, or by mistake, must not move the count.
     */
    fun episodePlayed(animeId: Long, episode: Int): Completable

    /** «Отметить все до N»: the count becomes [episode] when it is higher; never lowered here. */
    fun markWatchedUpTo(animeId: Long, episode: Int): Completable
}

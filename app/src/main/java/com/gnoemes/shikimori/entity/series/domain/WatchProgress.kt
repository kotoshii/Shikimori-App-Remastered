package com.gnoemes.shikimori.entity.series.domain

/**
 * How far the user is in one anime, as shikimori counts it: the list entry's id - null when the
 * title is not in the list - and its watched episodes count.
 */
data class WatchProgress(
        val rateId: Long?,
        val episodes: Int
) {
    companion object {
        val NOT_IN_LIST = WatchProgress(null, 0)
    }
}

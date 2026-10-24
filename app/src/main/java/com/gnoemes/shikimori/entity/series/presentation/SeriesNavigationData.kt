package com.gnoemes.shikimori.entity.series.presentation

import android.os.Parcelable
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.Status
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SeriesNavigationData(
        val animeId: Long,
        val image: Image,
        val name: String,
        val rateId: Long?,
        //the list entry's count when the screen was opened; WatchProgressStore keeps it current
        val watchedEpisodes : Int,
        //the episodes out by shikimori's count: every episode once the anime is released
        val episodesAired : Int,
        //how many the title will have; 0 when not known yet
        val episodes : Int,
        val episode : Int
) : Parcelable {

    /**
     * The last episode by shikimori's count, which the watch screen goes by (P7-T5 Q117/Q118): the
     * total when it is known, else the episodes out. What a source has beyond it is reached through
     * its episode list, the «# N» chip.
     */
    val lastEpisode: Int
        get() = if (episodes > 0) episodes else episodesAired

    companion object {
        /**
         * The watch screen of an anime, opened the same way from the list and from the title page: on
         * the episode after the last one watched, but not past the last one by shikimori's count, and
         * never below the first (BUGS #50; an announced title opened «# 0», #69 B6).
         */
        fun of(animeId: Long, image: Image, name: String, status: Status, episodes: Int, episodesAired: Int,
               rateId: Long?, watchedEpisodes: Int): SeriesNavigationData {
            val aired = if (status == Status.RELEASED) episodes else episodesAired
            val last = if (episodes > 0) episodes else aired
            return SeriesNavigationData(animeId, image, name, rateId, watchedEpisodes, aired, episodes,
                    (watchedEpisodes + 1).coerceAtMost(last).coerceAtLeast(1))
        }
    }
}
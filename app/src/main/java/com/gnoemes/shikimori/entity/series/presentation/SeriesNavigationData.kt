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
        val episodesAired : Int,
        val episode : Int?
) : Parcelable {

    companion object {
        /**
         * The watch screen of an anime, opened the same way from the list and from the title page.
         * It ends at the last episode out: every episode once the anime is released.
         */
        fun of(animeId: Long, image: Image, name: String, status: Status, episodes: Int, episodesAired: Int,
               rateId: Long?, watchedEpisodes: Int, episode: Int?) =
                SeriesNavigationData(animeId, image, name, rateId, watchedEpisodes,
                        if (status == Status.RELEASED) episodes else episodesAired, episode)
    }
}
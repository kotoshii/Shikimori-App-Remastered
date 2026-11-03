package com.gnoemes.shikimori.entity.series.presentation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class EpisodesNavigationData(
        val animeId: Long,
        val currentEpisode: Int,
        val isAlternative: Boolean
) : Parcelable
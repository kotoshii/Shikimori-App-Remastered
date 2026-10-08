package com.gnoemes.shikimori.entity.anime.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ScreenshotsNavigationData(
        val selected: Int,
        val items: List<Screenshot>,
        //names the folder the screenshots are saved in
        val animeName: String
) : Parcelable
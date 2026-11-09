package com.gnoemes.shikimori.entity.anime.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScreenshotsNavigationData(
        val selected: Int,
        val items: List<Screenshot>,
        //names the folder the screenshots are saved in: the title, or the topic an image in user text is from
        val name: String
) : Parcelable
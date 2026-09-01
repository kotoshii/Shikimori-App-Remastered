package com.gnoemes.shikimori.entity.series.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Video(
        val animeId: Long,
        val episodeId: Long,
        val player : String,
        val hosting: VideoHosting,
        val tracks: List<Track>,
        val subAss : String?,
        val subVtt : String?,
        //only used to name a download. Parsers do not set these - SeriesRepositoryImpl.getVideo
        //fills them in from the translation, so every hosting gets them without 13 edits.
        val author : String = "",
        val translationType : TranslationType? = null
) : Parcelable
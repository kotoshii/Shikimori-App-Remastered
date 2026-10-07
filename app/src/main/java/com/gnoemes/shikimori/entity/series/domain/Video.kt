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
        //only used to name a download. Parsers do not set these - SeriesRepositoryImpl.getVideo
        //fills them in from the translation, so every hosting gets them without an edit of its own.
        val author : String = "",
        val translationType : TranslationType? = null
) : Parcelable
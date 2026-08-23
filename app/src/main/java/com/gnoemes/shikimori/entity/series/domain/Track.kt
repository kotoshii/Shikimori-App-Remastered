package com.gnoemes.shikimori.entity.series.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Track(
        val quality : String,
        val url : String,
        //set when the hosting serves sound as its own file, see CdaParserImpl
        val audioUrl : String? = null
) : Parcelable
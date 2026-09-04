package com.gnoemes.shikimori.entity.search.presentation

import android.os.Parcelable
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SearchPayload(
        val genre: GenreV2? = null,
        val studioId: Long?= null
) : Parcelable
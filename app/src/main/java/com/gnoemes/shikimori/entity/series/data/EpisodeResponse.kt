package com.gnoemes.shikimori.entity.series.data

import com.google.gson.annotations.SerializedName

data class EpisodeResponse(
        @field:SerializedName("id") val id: Long,
        @field:SerializedName("index") val index: Int,
        @field:SerializedName("animeId") val animeId: Long
)
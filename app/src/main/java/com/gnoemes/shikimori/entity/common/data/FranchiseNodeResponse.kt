package com.gnoemes.shikimori.entity.common.data

import com.google.gson.annotations.SerializedName
import org.joda.time.DateTime

data class FranchiseNodeResponse(
        @field:SerializedName("id") val id: Long,
        @field:SerializedName("date") val date: DateTime,
        @field:SerializedName("name") val name: String,
        @field:SerializedName("image_url") val imageUrl: String?,
        @field:SerializedName("url") val url: String,
        @field:SerializedName("year") val year: Int?,
        @field:SerializedName("kind") val type: String?,
        @field:SerializedName("weight") val weight: Int
)
package com.gnoemes.shikimori.entity.publisher

import com.google.gson.annotations.SerializedName

/** A row of `/api/publishers`: a manga or ranobe publisher, mostly a magazine. */
data class PublisherResponse(
        @field:SerializedName("id") val id: Long,
        @field:SerializedName("name") val name: String
)

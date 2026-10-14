package com.gnoemes.shikimori.entity.user.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.joda.time.DateTime

/** Read by Gson as it is; [target] stays raw JSON, read as an anime or a manga by the converter. */
data class UserHistoryResponse(
        @field:SerializedName("id") val id : Long,
        @field:SerializedName("created_at") val dateCreated : DateTime,
        @field:SerializedName("description") val description : String,
        @field:SerializedName("target") val target : JsonElement?
)
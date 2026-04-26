package com.gnoemes.shikimori.entity.series.data

import com.google.gson.annotations.SerializedName

data class CdaApiRequest(
        @SerializedName("id") val id: Int,
        @SerializedName("jsonrpc") val jsonrpc: String,
        @SerializedName("method") val method: String,
        @SerializedName("params") val params: List<Any>
)

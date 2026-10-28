package com.gnoemes.shikimori.entity.series.data

import com.google.gson.annotations.SerializedName

/** A JSON-RPC call to cda.pl's own api; the app only asks `videoGetLink` (CdaParser). */
data class CdaApiRequest(
        @SerializedName("id") val id: Int,
        @SerializedName("jsonrpc") val jsonrpc: String,
        @SerializedName("method") val method: String,
        @SerializedName("params") val params: List<Any>
)

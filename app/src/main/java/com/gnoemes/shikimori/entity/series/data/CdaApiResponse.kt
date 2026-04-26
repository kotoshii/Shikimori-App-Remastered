package com.gnoemes.shikimori.entity.series.data

import com.google.gson.annotations.SerializedName

data class CdaApiResponse(
        @SerializedName("id") val id: Int,
        @SerializedName("jsonrpc") val jsonrpc: String,
        @SerializedName("result") val result: Result
) {
    data class Result(
            @SerializedName("status") val status: String,
            @SerializedName("resp") val resp: String
    )
}

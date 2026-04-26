package com.gnoemes.shikimori.entity.series.data

data class DzenPlayerData(
    val data: Data
) {
    data class Data(
        val content: Content
    ) {
        data class Content(
            val streams: List<Stream>
        ) {
            data class Stream(
                val url: String
            )
        }
    }
}
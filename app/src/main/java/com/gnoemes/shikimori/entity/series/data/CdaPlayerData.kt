package com.gnoemes.shikimori.entity.series.data

import com.google.gson.annotations.SerializedName

data class CdaPlayerData(
        @SerializedName("video") val video: Video
) {
    data class Video(
            @SerializedName("id") val id: String,
            @SerializedName("qualities") val qualities: Qualities,
            @SerializedName("ts") val ts: Long,
            @SerializedName("hash2") val hash2: String
    ) {
        data class Qualities (
                @SerializedName("360p")
                val q360: String?,

                @SerializedName("480p")
                val q480: String?,

                @SerializedName("720p")
                val q720: String?,

                @SerializedName("1080p")
                val q1080: String?
        )

        val cdaQualities: List<String>
            get() = listOfNotNull(
                    qualities.q360,
                    qualities.q480,
                    qualities.q720,
                    qualities.q1080
            )
    }
}

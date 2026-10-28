package com.gnoemes.shikimori.entity.series.data

import com.google.gson.annotations.SerializedName

data class CdaPlayerData(
        @SerializedName("video") val video: Video
) {
    data class Video(
            @SerializedName("id") val id: String,
            @SerializedName("qualities") val qualities: Qualities?,
            @SerializedName("ts") val ts: Long,
            @SerializedName("hash2") val hash2: String,
            //an hls playlist of one quality; most videos have no manifest any more (BUGS #19)
            @SerializedName("file") val file: String? = null,
            //the dash manifest, for the videos that still have one
            @SerializedName("manifest") val manifest: String? = null,
            //the real height of the video: a video with one quality lists it as "480p" whatever it is
            @SerializedName("height") val height: Int? = null
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

        /** Each listed quality as the app's label («720») and cda's code for it («sd»). */
        val cdaQualities: List<Pair<String, String>>
            get() = listOfNotNull(
                    qualities?.q360?.let { "360" to it },
                    qualities?.q480?.let { "480" to it },
                    qualities?.q720?.let { "720" to it },
                    qualities?.q1080?.let { "1080" to it }
            )
    }
}

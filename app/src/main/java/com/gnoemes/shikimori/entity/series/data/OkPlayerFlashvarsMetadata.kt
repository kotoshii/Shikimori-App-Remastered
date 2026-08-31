package com.gnoemes.shikimori.entity.series.data

import com.google.gson.annotations.SerializedName

data class OkPlayerFlashvarsMetadata(
        /**
         * Progressive files. ok.ru has cut this down to the single `full` rendition, so the whole
         * quality ladder now lives in [ondemandHls] instead.
         */
        @SerializedName("videos") val videos: List<Video>?,

        /** Master playlist carrying every quality ok.ru encoded. */
        @SerializedName("ondemandHls") val ondemandHls: String?
) {

    data class Video(
            @SerializedName("name") val name: String?,
            @SerializedName("url") val url: String?
    )
}

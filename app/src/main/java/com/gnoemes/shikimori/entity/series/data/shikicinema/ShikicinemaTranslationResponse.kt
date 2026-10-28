package com.gnoemes.shikimori.entity.series.data.shikicinema

import com.gnoemes.shikimori.entity.series.domain.TranslationQuality
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import com.google.gson.annotations.SerializedName
import java.net.URL

//smarthard.net answers in snake_case on a cache miss and in camelCase on a cache hit (x-cache: HIT),
//so the two renamed keys accept both spellings
data class ShikicinemaTranslationResponse (
        @SerializedName("id") val id: Long,
        @SerializedName("url") val url: String,
        @SerializedName("anime_id", alternate = ["animeId"]) val anime_id: Long,
        @SerializedName("anime_english") private val anime_english: String,
        @SerializedName("anime_russian") private val anime_russian: String,
        @SerializedName("episode") val episode: Int,
        @SerializedName("kind") private val _kind: String,
        @SerializedName("language") private val language: String,
        @SerializedName("quality") val _quality: String?,
        @SerializedName("author") val author: String?,
        @SerializedName("watches_count", alternate = ["watchesCount"]) val watches_count: Long?,
        @SerializedName("uploader") val uploader: String
) {
    val quality: TranslationQuality
        get() = when (_quality?.toLowerCase()) {
            "bd" -> TranslationQuality.BD
            "dvd" -> TranslationQuality.DVD
            else -> TranslationQuality.TV
        }

    val kind: TranslationType
        get() = listOf(TranslationType.SUB_RU, TranslationType.VOICE_RU).find { it.shikicinemaKind == _kind }
                ?: TranslationType.RAW

    /** The player's address, on its current domain if the one Shikicinema names has moved. */
    val playerUrl: String
        get() = VideoHosting.currentUrl(url)

    val hosting: String
        get() = URL(playerUrl).host
}

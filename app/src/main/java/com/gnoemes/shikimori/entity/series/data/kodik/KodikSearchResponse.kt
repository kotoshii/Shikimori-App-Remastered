package com.gnoemes.shikimori.entity.series.data.kodik

import com.gnoemes.shikimori.entity.series.domain.TranslationQuality
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import com.google.gson.annotations.SerializedName

/**
 * Answer of `kodik-api.com/search`. Only the fields the app needs are mapped, everything is
 * nullable because kodik leaves plenty of them out depending on the kind of title.
 */
data class KodikSearchResponse(
        @SerializedName("total") val total: Int?,
        @SerializedName("results") val results: List<Result>?
) {

    data class Result(
            @SerializedName("id") val id: String?,
            @SerializedName("link") val link: String?,
            @SerializedName("translation") val translation: Translation?,
            @SerializedName("last_season") val lastSeason: Int?,
            @SerializedName("last_episode") val lastEpisode: Int?,
            @SerializedName("episodes_count") val episodesCount: Int?,
            @SerializedName("quality") private val _quality: String?,
            @SerializedName("seasons") val seasons: Map<String, Season>?
    ) {

        /**
         * A movie is a single file, so it has no season map at all - its only link is the result's
         * own one.
         */
        val isMovie: Boolean
            get() = id?.contains("movie") == true

        val episodes: Int
            get() = when {
                isMovie -> 1
                lastEpisode != null -> lastEpisode
                episodesCount != null -> episodesCount
                else -> 0
            }

        /**
         * Kodik writes free text here - "WEB-DLRip 720p", "BDRip", "DVDRip" - while the app only
         * knows three grades.
         */
        val quality: TranslationQuality
            get() = when {
                _quality == null -> TranslationQuality.TV
                _quality.contains("bd", true) || _quality.contains("blu", true) -> TranslationQuality.BD
                _quality.contains("dvd", true) -> TranslationQuality.DVD
                else -> TranslationQuality.TV
            }

        /**
         * Episodes are looked up in the last season, which is what a shikimori id maps to - each
         * season of a franchise is its own title there, and so its own search result.
         */
        fun episodeUrl(episode: Long): String? =
                if (isMovie) link
                else seasons?.get(lastSeason?.toString())?.episodes?.get(episode.toString())

        data class Translation(
                @SerializedName("title") val title: String?,
                @SerializedName("type") private val _type: String?
        ) {
            val type: TranslationType
                get() = when (_type) {
                    "voice" -> TranslationType.VOICE_RU
                    "subtitles" -> TranslationType.SUB_RU
                    else -> TranslationType.RAW
                }
        }

        data class Season(
                @SerializedName("episodes") val episodes: Map<String, String>?
        )
    }
}

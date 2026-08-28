package com.gnoemes.shikimori.data.network

import com.gnoemes.shikimori.entity.series.data.EpisodeResponse
import com.gnoemes.shikimori.entity.series.data.TranslationResponse
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import io.reactivex.Single

/**
 * Episodes and translations for the two sources the app has: Kodik (the plain calls) and
 * Shikicinema (the `*Shikicinema` ones). Video resolution is **not** here - every hosting is parsed
 * client side by `SeriesRepositoryImpl`, so there is nothing left for a source to resolve.
 */
interface AnimeSource {

    fun getEpisodes(id: Long, name : String): Single<List<EpisodeResponse>>

    fun getEpisodesShikicinema(id: Long): Single<List<EpisodeResponse>>

    fun getTranslations(animeId: Long, name: String, episodeId: Long, type: TranslationType): Single<List<TranslationResponse>>

    fun getTranslationsShikicinema(animeId: Long, episode: Long, type: TranslationType, loadLength: Boolean): Single<List<TranslationResponse>>
}
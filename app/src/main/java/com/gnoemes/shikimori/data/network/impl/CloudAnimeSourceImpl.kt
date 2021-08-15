package com.gnoemes.shikimori.data.network.impl

import com.gnoemes.shikimori.data.network.AnimeSource
import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.entity.series.data.EpisodeResponse
import com.gnoemes.shikimori.entity.series.data.TranslationResponse
import com.gnoemes.shikimori.entity.series.data.VideoResponse
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import io.reactivex.Single
import javax.inject.Inject

class CloudAnimeSourceImpl @Inject constructor(private val api: VideoApi) : AnimeSource {

    override fun getEpisodes(id: Long, name: String): Single<List<EpisodeResponse>> = api.getEpisodes(id)

    override fun getEpisodesAlternative(id: Long): Single<List<EpisodeResponse>> = api.getEpisodesAlternative(id)

    override fun getTranslations(animeId: Long, name: String, episodeId: Long, type: TranslationType): Single<List<TranslationResponse>> = api.getTranslations(animeId, episodeId, type.type!!)

    override fun getTranslationsAlternative(animeId: Long, episodeId: Long, type: String): Single<List<TranslationResponse>> = api.getTranslationsAlternative(animeId, episodeId, type)

    override fun getVideo(animeId: Long, episodeId: Int, videoId: String, language: String, type: TranslationType, author: String, hosting: String, url : String?): Single<VideoResponse> = api.getVideo(animeId, episodeId, videoId, language, type.type!!, author, hosting)

    override fun getVideoAlternative(translationId: Long, animeId: Long, episodeIndex: Long, token : String?): Single<VideoResponse> = api.getVideoAlternative(translationId, token)
            .map { it.copy(animeId = animeId, episodeId = episodeIndex) }
}
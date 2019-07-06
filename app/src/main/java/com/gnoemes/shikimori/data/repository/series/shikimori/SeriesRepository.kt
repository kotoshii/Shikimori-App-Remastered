package com.gnoemes.shikimori.data.repository.series.shikimori

import com.gnoemes.shikimori.entity.series.domain.Episode
import com.gnoemes.shikimori.entity.series.domain.Translation
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import io.reactivex.Completable
import io.reactivex.Single

interface SeriesRepository {

    fun getEpisodes(id: Long, alternative: Boolean): Single<List<Episode>>

    fun getTranslations(type: TranslationType, animeId: Long, episodeId: Long, alternative: Boolean): Single<List<Translation>>

    fun getVideo(payload: TranslationVideo, alternative: Boolean): Single<Video>

    fun setEpisodeStatus(animeId: Long, episodeId: Int, isWatched: Boolean): Completable

    fun isEpisodeWatched(animeId: Long, episodeId: Int): Single<Boolean>

    fun getTopic(animeId: Long, episodeId: Int): Single<Long>

    fun getFirstNotWatchedEpisodeIndex(animeId: Long): Single<Int>

    fun getWatchedEpisodesCount(animeId: Long): Single<Int>

}
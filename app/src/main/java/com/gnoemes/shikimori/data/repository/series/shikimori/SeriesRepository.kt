package com.gnoemes.shikimori.data.repository.series.shikimori

import com.gnoemes.shikimori.entity.series.domain.Episode
import com.gnoemes.shikimori.entity.series.domain.Translation
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import io.reactivex.Single

interface SeriesRepository {

    fun getEpisodes(id: Long, alternative: Boolean): Single<List<Episode>>

    fun getTranslations(type: TranslationType, animeId: Long, episodeId: Long, alternative: Boolean, loadLength: Boolean): Single<List<Translation>>

    fun getVideo(payload: TranslationVideo): Single<Video>

    fun getTopic(animeId: Long, episodeId: Int): Single<Long>

}
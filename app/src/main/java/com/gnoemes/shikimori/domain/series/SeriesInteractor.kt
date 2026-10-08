package com.gnoemes.shikimori.domain.series

import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import io.reactivex.Completable
import io.reactivex.Single

interface SeriesInteractor {

    fun getEpisodes(id : Long, name : String, alternative : Boolean) : Single<List<Episode>>

    fun getTranslations(type: TranslationType, animeId: Long, episodeId: Long, name : String, alternative: Boolean, loadLength: Boolean): Single<List<Translation>>

    fun getTranslationSettings(animeId: Long) : Single<TranslationSetting>

    fun saveTranslationSettings(settings : TranslationSetting) : Completable

    fun getVideo(payload : TranslationVideo) : Single<Video>

    fun getTopic(animeId: Long, episodeId: Int) : Single<Long>
}
package com.gnoemes.shikimori.domain.series

import com.gnoemes.shikimori.data.local.progress.WatchProgressStore
import com.gnoemes.shikimori.data.repository.progress.AnimeProgressRepository
import com.gnoemes.shikimori.data.repository.series.shikimori.SeriesRepository
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class SeriesInteractorImpl @Inject constructor(
        private val repository: SeriesRepository,
        private val progressRepository: AnimeProgressRepository,
        private val watchProgress: WatchProgressStore
) : SeriesInteractor {

    override fun getEpisodes(id: Long, alternative: Boolean): Single<List<Episode>> =
            repository.getEpisodes(id, alternative)
                    //watched means not above shikimori's count - the app keeps no ticks of its own
                    .map { list ->
                        val watched = watchProgress.watchedEpisodes(id)
                        list.sortedBy { it.index }.map { it.copy(isWatched = it.index <= watched) }
                    }
                    .applyErrorHandlerAndSchedulers()

    override fun getTranslations(type: TranslationType, animeId: Long, episodeId: Long, alternative: Boolean, loadLength: Boolean): Single<List<Translation>> =
            repository.getTranslations(type, animeId, episodeId, alternative, loadLength)
                    .applyErrorHandlerAndSchedulers()

    override fun getTranslationSettings(animeId: Long): Single<TranslationSetting> =
            progressRepository.getTranslationSettings(animeId)
                    .applyErrorHandlerAndSchedulers()

    override fun saveTranslationSettings(settings: TranslationSetting): Completable =
            progressRepository.saveTranslationSettings(settings)
                    .applyErrorHandlerAndSchedulers()

    override fun getVideo(payload: TranslationVideo): Single<Video> =
            repository.getVideo(payload)
                    .applyErrorHandlerAndSchedulers()

    override fun getTopic(animeId: Long, episodeId: Int): Single<Long> =
            repository.getTopic(animeId, episodeId)
                    .applyErrorHandlerAndSchedulers()

}
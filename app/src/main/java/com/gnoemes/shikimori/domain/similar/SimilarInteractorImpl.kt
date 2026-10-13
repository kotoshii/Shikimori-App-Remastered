package com.gnoemes.shikimori.domain.similar

import com.gnoemes.shikimori.data.repository.anime.AnimeRepository
import com.gnoemes.shikimori.data.repository.manga.MangaRepository
import com.gnoemes.shikimori.data.repository.rates.RatesRepository
import com.gnoemes.shikimori.entity.anime.domain.AnimeWithStatus
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.MangaWithStatus
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Single
import javax.inject.Inject

class SimilarInteractorImpl @Inject constructor(
        private val animeRepository: AnimeRepository,
        private val mangaRepository: MangaRepository,
        private val rateRepository: RatesRepository
) : SimilarInteractor {

    override fun getAnimes(animeId: Long): Single<List<AnimeWithStatus>> =
            animeRepository.getSimilar(animeId)
                    .flatMap { list ->
                        rateRepository.getMyRates(Type.ANIME)
                                .map { rates -> list.map { AnimeWithStatus(it, rates[it.id]?.id, rates[it.id]?.status) } }
                    }
                    .applyErrorHandlerAndSchedulers()

    override fun getMangas(mangaId: Long, type: Type): Single<List<MangaWithStatus>> =
            mangaRepository.getSimilar(mangaId, type)
                    .flatMap { list ->
                        rateRepository.getMyRates(type)
                                .map { rates -> list.map { MangaWithStatus(it, rates[it.id]?.id, rates[it.id]?.status) } }
                    }
                    .applyErrorHandlerAndSchedulers()
}
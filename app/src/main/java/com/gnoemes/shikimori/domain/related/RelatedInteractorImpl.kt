package com.gnoemes.shikimori.domain.related

import com.gnoemes.shikimori.data.repository.related.RelatedRepository
import com.gnoemes.shikimori.entity.common.domain.Related
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Single
import javax.inject.Inject

class RelatedInteractorImpl @Inject constructor(
        private val repository: RelatedRepository
) : RelatedInteractor {

    override fun getAnime(animeId: Long): Single<List<Related>> = repository.getAnime(animeId).applyErrorHandlerAndSchedulers()

    override fun getManga(mangaId: Long, type: Type): Single<List<Related>> = repository.getManga(mangaId, type).applyErrorHandlerAndSchedulers()
}

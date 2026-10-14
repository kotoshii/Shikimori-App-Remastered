package com.gnoemes.shikimori.domain.manga

import com.gnoemes.shikimori.data.repository.manga.MangaRepository
import com.gnoemes.shikimori.entity.common.domain.Link
import com.gnoemes.shikimori.entity.common.domain.Roles
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.MangaDetails
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Single
import javax.inject.Inject

class MangaInteractorImpl @Inject constructor(
        private val repository: MangaRepository
) : MangaInteractor {

    override fun getDetails(id: Long, type: Type): Single<MangaDetails> =
            repository.getDetails(id, type)
                    .applyErrorHandlerAndSchedulers()

    override fun getRoles(id: Long, type: Type): Single<Roles> =
            repository.getRoles(id, type)
                    .applyErrorHandlerAndSchedulers()

    override fun getLinks(id: Long, type: Type): Single<List<Link>> =
            repository.getLinks(id, type)
                    .applyErrorHandlerAndSchedulers()
}

package com.gnoemes.shikimori.data.repository.manga

import com.gnoemes.shikimori.entity.common.domain.Franchise
import com.gnoemes.shikimori.entity.common.domain.Link
import com.gnoemes.shikimori.entity.common.domain.Roles
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.manga.domain.MangaDetails
import io.reactivex.Single

/** Manga and ranobe alike; [Type] MANGA or RANOBE picks which. */
interface MangaRepository {

    fun getDetails(id: Long, type: Type): Single<MangaDetails>

    fun getRoles(id: Long, type: Type): Single<Roles>

    fun getLinks(id: Long, type: Type): Single<List<Link>>

    fun getSimilar(id: Long, type: Type): Single<List<Manga>>

    fun getFranchise(id: Long, type: Type): Single<Franchise>
}

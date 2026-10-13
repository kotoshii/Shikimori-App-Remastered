package com.gnoemes.shikimori.domain.manga

import com.gnoemes.shikimori.entity.common.domain.Link
import com.gnoemes.shikimori.entity.common.domain.Roles
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.MangaDetails
import io.reactivex.Single

/** Manga and ranobe alike; [Type] MANGA or RANOBE picks which. */
interface MangaInteractor {

    fun getDetails(id: Long, type: Type): Single<MangaDetails>

    fun getRoles(id: Long, type: Type): Single<Roles>

    fun getLinks(id: Long, type: Type): Single<List<Link>>
}

package com.gnoemes.shikimori.domain.related

import com.gnoemes.shikimori.entity.common.domain.Related
import com.gnoemes.shikimori.entity.common.domain.Type
import io.reactivex.Single

interface RelatedInteractor {

    fun getAnime(animeId: Long): Single<List<Related>>

    /** [type] MANGA or RANOBE. */
    fun getManga(mangaId: Long, type: Type): Single<List<Related>>
}

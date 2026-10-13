package com.gnoemes.shikimori.domain.similar

import com.gnoemes.shikimori.entity.anime.domain.AnimeWithStatus
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.MangaWithStatus
import io.reactivex.Single

interface SimilarInteractor {

    fun getAnimes(animeId: Long): Single<List<AnimeWithStatus>>

    /** [type] MANGA or RANOBE. */
    fun getMangas(mangaId: Long, type: Type): Single<List<MangaWithStatus>>

}
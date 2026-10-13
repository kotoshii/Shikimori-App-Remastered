package com.gnoemes.shikimori.domain.chronology

import com.gnoemes.shikimori.entity.chronology.ChronologyItem
import com.gnoemes.shikimori.entity.chronology.ChronologyType
import com.gnoemes.shikimori.entity.common.domain.Type
import io.reactivex.Single

interface ChronologyInteractor {
    fun getAnimes(id: Long, type : ChronologyType): Single<List<ChronologyItem>>
    /** [type] MANGA or RANOBE. */
    fun getMangas(id: Long, type: Type, chronologyType: ChronologyType): Single<List<ChronologyItem>>
}
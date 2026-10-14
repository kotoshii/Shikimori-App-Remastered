package com.gnoemes.shikimori.domain.chronology

import com.gnoemes.shikimori.entity.chronology.ChronologyItem
import com.gnoemes.shikimori.entity.chronology.ChronologyType
import com.gnoemes.shikimori.entity.common.domain.Type
import io.reactivex.Single

interface ChronologyInteractor {

    /** The franchise of title [id] ([type] ANIME, MANGA or RANOBE) as [chronologyType] shows it. */
    fun getItems(id: Long, type: Type, chronologyType: ChronologyType): Single<List<ChronologyItem>>
}

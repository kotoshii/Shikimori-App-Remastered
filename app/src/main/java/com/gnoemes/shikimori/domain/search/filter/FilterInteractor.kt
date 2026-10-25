package com.gnoemes.shikimori.domain.search.filter

import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import io.reactivex.Single

interface FilterInteractor {
    fun getAnimeFilters(): Single<List<FilterCategory>>
    fun getMangaFilters(): Single<List<FilterCategory>>
    fun getRanobeFilters(): Single<List<FilterCategory>>

    fun getAnimeSortFilters() : Single<List<FilterItem>>
    fun getMangaSortFilters() : Single<List<FilterItem>>

    /** The filter categories of [type]: anime, manga and ranobe each have their own. */
    fun getFilters(type: Type): Single<List<FilterCategory>>

    /** The sorts of [type]. */
    fun getSortFilters(type: Type): Single<List<FilterItem>>
}
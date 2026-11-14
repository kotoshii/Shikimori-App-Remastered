package com.gnoemes.shikimori.data.repository.search

import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import io.reactivex.Single

interface FilterRepository {
    fun getAnimeFilters(): Single<List<FilterCategory>>
    fun getMangaFilters(): Single<List<FilterCategory>>
    fun getRanobeFilters(): Single<List<FilterCategory>>

    fun getAnimeSortFilters() : Single<List<FilterItem>>
    fun getMangaSortFilters() : Single<List<FilterItem>>

    /** The studios ([FilterType.STUDIO]) or publishers ([FilterType.PUBLISHER]) to pick from, by name. */
    fun getCompanies(filter: FilterType): Single<List<FilterItem>>

    /** The same list when it is kept in memory already, else null. */
    fun getKeptCompanies(filter: FilterType): List<FilterItem>?
}
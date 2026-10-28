package com.gnoemes.shikimori.domain.search

import com.gnoemes.shikimori.entity.common.domain.FilterItem
import io.reactivex.Single

interface SearchQueryBuilder {

    fun createQueryFromFilters(filters: Map<String, MutableList<FilterItem>>?, page: Int?, limit: Int?): Single<Map<String, String>>

    /** An anime, manga or ranobe list: [createQueryFromFilters], sorted by the default when no sort is chosen. */
    fun createTitleListQuery(filters: Map<String, MutableList<FilterItem>>?, page: Int, limit: Int): Single<Map<String, String>>

    fun createQueryFromIds(ids: MutableCollection<Long>): Single<Map<String, String>>
}
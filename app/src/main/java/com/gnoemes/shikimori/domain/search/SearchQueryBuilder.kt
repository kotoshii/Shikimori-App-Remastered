package com.gnoemes.shikimori.domain.search

import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import io.reactivex.Single

interface SearchQueryBuilder {

    fun createQueryFromFilters(filters: Map<String, MutableList<FilterItem>>?, page: Int?, limit: Int?): Single<Map<String, String>>

    fun createQueryFromIds(ids: MutableCollection<Long>, searchQuery: String? = null, page: Int = 1, limit: Int = Constants.MAX_LIMIT): Single<Map<String, String>>
}
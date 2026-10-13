package com.gnoemes.shikimori.data.repository.search

import com.gnoemes.shikimori.data.local.services.FilterSource
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import io.reactivex.Single
import javax.inject.Inject

class FilterRepositoryImpl @Inject constructor(
        private val source: FilterSource
) : FilterRepository {

    //fromCallable, not just: the lists are built when subscribed, on the io scheduler - the first
    //one parses the bundled genre file
    override fun getAnimeFilters(): Single<List<FilterCategory>> = Single.fromCallable { source.getAnimeFilters() }
    override fun getMangaFilters(): Single<List<FilterCategory>> = Single.fromCallable { source.getMangaFilters() }
    override fun getRanobeFilters(): Single<List<FilterCategory>> = Single.fromCallable { source.getRanobeFilters() }

    override fun getAnimeSortFilters(): Single<List<FilterItem>> = Single.fromCallable { source.getAnimeSortFilters() }
    override fun getMangaSortFilters(): Single<List<FilterItem>> = Single.fromCallable { source.getMangaSortFilters() }
}
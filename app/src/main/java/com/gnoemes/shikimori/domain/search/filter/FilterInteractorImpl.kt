package com.gnoemes.shikimori.domain.search.filter

import com.gnoemes.shikimori.data.repository.search.FilterRepository
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Single
import javax.inject.Inject

class FilterInteractorImpl @Inject constructor(
        private val repository: FilterRepository
) : FilterInteractor {

    override fun getAnimeFilters(): Single<List<FilterCategory>> = repository.getAnimeFilters().applyErrorHandlerAndSchedulers()
    override fun getMangaFilters(): Single<List<FilterCategory>> = repository.getMangaFilters().applyErrorHandlerAndSchedulers()
    override fun getRanobeFilters(): Single<List<FilterCategory>> = repository.getRanobeFilters().applyErrorHandlerAndSchedulers()

    override fun getFilters(type: Type): Single<List<FilterCategory>> = when (type) {
        Type.MANGA -> getMangaFilters()
        Type.RANOBE -> getRanobeFilters()
        else -> getAnimeFilters()
    }

    //ranobe sorts as manga does: it has chapters and volumes, not episodes, and the api answers an
    //episodes sort for it with 422 (BUGS #10)
    override fun getSortFilters(type: Type): Single<List<FilterItem>> = when (type) {
        Type.MANGA, Type.RANOBE -> getMangaSortFilters()
        else -> getAnimeSortFilters()
    }

    override fun getAnimeSortFilters(): Single<List<FilterItem>> = repository.getAnimeSortFilters().applyErrorHandlerAndSchedulers()
    override fun getMangaSortFilters(): Single<List<FilterItem>> = repository.getMangaSortFilters().applyErrorHandlerAndSchedulers()
}
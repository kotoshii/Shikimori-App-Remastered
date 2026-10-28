package com.gnoemes.shikimori.domain.search

import com.gnoemes.shikimori.data.repository.search.SearchRepository
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.roles.domain.Character
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Single
import javax.inject.Inject

class SearchInteractorImpl @Inject constructor(
        private val repository: SearchRepository,
        private val queryBuilder: SearchQueryBuilder
) : SearchInteractor {

    override fun loadCharacterListWithFilters(filters: HashMap<String, MutableList<FilterItem>>?): Single<List<Character>> =
            queryBuilder.createQueryFromFilters(filters, null, null)
                    .flatMap { repository.getCharacterList(it) }
                    .applyErrorHandlerAndSchedulers()

    override fun loadPersonListWithFilters(filters: HashMap<String, MutableList<FilterItem>>?): Single<List<Person>> =
            queryBuilder.createQueryFromFilters(filters, null, null)
                    .flatMap { repository.getPersonList(it) }
                    .applyErrorHandlerAndSchedulers()

    //a page of the catalog: exactly `limit` titles, so a shorter one is the last. Made here, where the
    //limit is known; the converters keep one title per row (manga drops only null rows)
    override fun loadAnimeListWithFilters(filters: HashMap<String, MutableList<FilterItem>>?, page: Int, limit: Int): Single<Page<Anime>> =
            queryBuilder.createTitleListQuery(filters, page, limit)
                    .flatMap { repository.getAnimeList(it) }
                    .map { Page.exact(it, limit) }
                    .applyErrorHandlerAndSchedulers()

    override fun loadMangaListWithFilters(filters: HashMap<String, MutableList<FilterItem>>?, type: Type, page: Int, limit: Int): Single<Page<Manga>> =
            queryBuilder.createTitleListQuery(filters, page, limit)
                    .flatMap { repository.getMangaList(it, type) }
                    .map { Page.exact(it, limit) }
                    .applyErrorHandlerAndSchedulers()

}
package com.gnoemes.shikimori.data.repository.search

import com.gnoemes.shikimori.data.network.AnimeApi
import com.gnoemes.shikimori.data.network.GraphqlSearchApi
import com.gnoemes.shikimori.data.network.MangaApi
import com.gnoemes.shikimori.data.network.RanobeApi
import com.gnoemes.shikimori.data.network.RolesApi
import com.gnoemes.shikimori.data.repository.common.AnimeResponseConverter
import com.gnoemes.shikimori.data.repository.common.CharacterResponseConverter
import com.gnoemes.shikimori.data.repository.common.GraphqlContentConverter
import com.gnoemes.shikimori.data.repository.common.MangaResponseConverter
import com.gnoemes.shikimori.data.repository.common.PersonResponseConverter
import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlRequest
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.common.data.graphql.SearchQueryResponse
import com.gnoemes.shikimori.entity.common.domain.LinkedContent
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.roles.domain.Character
import com.gnoemes.shikimori.entity.roles.domain.Person
import io.reactivex.Single
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
        private val animesApi: AnimeApi,
        private val mangaApi: MangaApi,
        private val ranobeApi: RanobeApi,
        private val rolesApi: RolesApi,
        private val graphqlSearchApi: GraphqlSearchApi,
        private val graphqlConverter: GraphqlContentConverter,
        private val animeResponseConverter: AnimeResponseConverter,
        private val mangaResponseConverter: MangaResponseConverter,
        private val characterResponseConverter: CharacterResponseConverter,
        private val personResponseConverter: PersonResponseConverter
) : SearchRepository {

    /**
     * Anime, manga and ranobe searches go through graphql, because **genre filtering only works
     * there**: shikimori's v2 genres are not exposed by the rest api, and a v2 genre id sent to
     * `/api/animes` matches nothing (five ids even mean something else there). See
     * docs/_internal/GENRES_V2_SPIKE.md.
     *
     * The whole catalog moved rather than only genre-filtered searches - one screen paging through
     * two apis would order its results differently depending on which filters were set.
     *
     * `animesApi`, `mangaApi`, `ranobeApi` and their converters are left injected although nothing
     * in this class calls them any more - the interfaces themselves are still used elsewhere for
     * details, roles, similar and franchise, and nothing is removed without asking.
     */
    override fun getAnimeList(queryMap: Map<String, String>): Single<List<Anime>> =
            searchGraphql(GenreEntryType.ANIME, queryMap, isRanobe = false)
                    .map { graphqlConverter.convertAnimes(it.data?.animes) }

    override fun getMangaList(queryMap: Map<String, String>): Single<List<Manga>> =
            searchGraphql(GenreEntryType.MANGA, queryMap, isRanobe = false)
                    .map { graphqlConverter.convertMangas(it.data?.mangas) }

    override fun getRanobeList(queryMap: Map<String, String>): Single<List<Manga>> =
            searchGraphql(GenreEntryType.MANGA, queryMap, isRanobe = true)
                    .map { graphqlConverter.convertMangas(it.data?.mangas) }

    private fun searchGraphql(
            type: GenreEntryType,
            queryMap: Map<String, String>,
            isRanobe: Boolean
    ): Single<SearchQueryResponse> = Single
            .fromCallable { GraphqlSearchQuery.build(type, queryMap, isRanobe) }
            .flatMap { query -> graphqlSearchApi.search(GraphqlRequest(query)) }

    override fun getCharacterList(queryMap: Map<String, String>): Single<List<Character>> =
            rolesApi.getCharacterList(queryMap)
                    .map(characterResponseConverter)

    override fun getPersonList(queryMap: Map<String, String>): Single<List<Person>> =
            rolesApi.getPersonList(queryMap)
                    .map(personResponseConverter)

    override fun getList(type: Type, queryMap: Map<String, String>): Single<List<LinkedContent>> =
            (when (type) {
                Type.ANIME -> getAnimeList(queryMap)
                Type.MANGA -> getMangaList(queryMap)
                Type.RANOBE -> getRanobeList(queryMap)
                Type.CHARACTER -> getCharacterList(queryMap)
                Type.PERSON -> getCharacterList(queryMap)
                else -> Single.error(IllegalArgumentException("$type search is not supported"))
            })
                    .map { it }

}
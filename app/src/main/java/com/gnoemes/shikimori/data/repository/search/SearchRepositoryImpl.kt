package com.gnoemes.shikimori.data.repository.search

import com.gnoemes.shikimori.data.network.AnimeApi
import com.gnoemes.shikimori.data.network.MangaApi
import com.gnoemes.shikimori.data.network.RanobeApi
import com.gnoemes.shikimori.data.network.RolesApi
import com.gnoemes.shikimori.data.repository.common.AnimeResponseConverter
import com.gnoemes.shikimori.data.repository.common.CharacterResponseConverter
import com.gnoemes.shikimori.data.repository.common.MangaResponseConverter
import com.gnoemes.shikimori.data.repository.common.PersonResponseConverter
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.common.domain.LinkedContent
import com.gnoemes.shikimori.entity.common.domain.SearchConstants
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
        private val animeResponseConverter: AnimeResponseConverter,
        private val mangaResponseConverter: MangaResponseConverter,
        private val characterResponseConverter: CharacterResponseConverter,
        private val personResponseConverter: PersonResponseConverter
) : SearchRepository {

    /**
     * Anime, manga and ranobe searches go through rest, with the genre filter sent as `genre_v2`
     * (see [withGenreV2]). Rest has the real posters of titles shikimori now gates as 18+ (yuri,
     * yaoi, hentai, shoujo-ai/shounen-ai), which graphql answers with a null poster even with the
     * user's token.
     */
    override fun getAnimeList(queryMap: Map<String, String>): Single<List<Anime>> =
            animesApi.getList(withGenreV2(queryMap))
                    .map(animeResponseConverter)

    override fun getMangaList(queryMap: Map<String, String>): Single<List<Manga>> =
            mangaApi.getList(withGenreV2(queryMap))
                    .map(mangaResponseConverter)

    override fun getRanobeList(queryMap: Map<String, String>): Single<List<Manga>> =
            ranobeApi.getList(withGenreV2(queryMap))
                    .map(mangaResponseConverter)

    /**
     * The filter holds v2 genre ids, and rest's `genre` only knows the v1 vocabulary - five ids
     * even mean something else there. `genre_v2` takes the same comma separated ids with the same
     * `!` exclusion prefix, returns what the graphql catalog did in the same order, and for manga
     * and ranobe filters every v2 genre, where graphql only manages 40 of 81. Verified against the
     * live api on 2026-10-01. See docs/_internal/GENRES_V2_SPIKE.md.
     */
    private fun withGenreV2(queryMap: Map<String, String>): Map<String, String> {
        val genre = queryMap[SearchConstants.GENRE] ?: return queryMap
        return queryMap - SearchConstants.GENRE + (SearchConstants.GENRE_V2 to genre)
    }

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
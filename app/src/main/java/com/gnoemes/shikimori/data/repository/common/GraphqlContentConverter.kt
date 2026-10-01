package com.gnoemes.shikimori.data.repository.common

import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlAnimeResponse
import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlMangaResponse
import com.gnoemes.shikimori.entity.manga.domain.Manga

/**
 * Graphql catalog items into the same domain models the REST list produced, so nothing above the
 * repository can tell which api answered.
 */
interface GraphqlContentConverter {

    fun convertAnimes(list: List<GraphqlAnimeResponse>?): List<Anime>

    fun convertMangas(list: List<GraphqlMangaResponse>?): List<Manga>
}

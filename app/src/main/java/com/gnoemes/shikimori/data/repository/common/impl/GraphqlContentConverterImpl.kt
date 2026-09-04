package com.gnoemes.shikimori.data.repository.common.impl

import com.gnoemes.shikimori.data.repository.common.GraphqlContentConverter
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlAnimeResponse
import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlMangaResponse
import com.gnoemes.shikimori.entity.common.data.graphql.PosterResponse
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.utils.nullIfEmpty
import javax.inject.Inject

class GraphqlContentConverterImpl @Inject constructor() : GraphqlContentConverter {

    override fun convertAnimes(list: List<GraphqlAnimeResponse>?): List<Anime> =
            list.orEmpty().mapNotNull { response ->
                val id = response.id?.toLongOrNull() ?: return@mapNotNull null

                Anime(
                        id,
                        response.name.orEmpty().trim(),
                        response.nameRu?.trim().nullIfEmpty(),
                        convertPoster(response.poster),
                        //graphql urls are absolute, unlike the rest ones appendHostIfNeed patched
                        response.url.orEmpty(),
                        response.type,
                        response.score,
                        response.status,
                        response.episodes ?: 0,
                        response.episodesAired ?: 0,
                        response.dateAired?.date,
                        response.dateReleased?.date
                )
            }

    override fun convertMangas(list: List<GraphqlMangaResponse>?): List<Manga> =
            list.orEmpty().mapNotNull { response ->
                val id = response.id?.toLongOrNull() ?: return@mapNotNull null

                Manga(
                        id,
                        response.name.orEmpty().trim(),
                        response.nameRu?.trim().nullIfEmpty(),
                        convertPoster(response.poster),
                        response.url.orEmpty(),
                        response.type,
                        response.score,
                        response.status,
                        response.volumes ?: 0,
                        response.chapters ?: 0,
                        response.dateAired?.date,
                        response.dateReleased?.date
                )
            }

    /**
     * [PosterResponse.urlFor] already maps a REST size name onto the closest graphql rendition and
     * prefers the jpeg/png variants, so the [Image] that comes out is what the REST one held -
     * except that it is never a `missing_*` placeholder, which is the whole reason
     * `MissingPosterInterceptor` exists for the REST list.
     */
    private fun convertPoster(poster: PosterResponse?): Image = Image(
            poster?.urlFor("original"),
            poster?.urlFor("preview"),
            poster?.urlFor("x96"),
            poster?.urlFor("x48")
    )
}

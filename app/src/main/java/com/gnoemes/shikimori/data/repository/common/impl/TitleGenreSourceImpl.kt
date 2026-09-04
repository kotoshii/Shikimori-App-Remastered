package com.gnoemes.shikimori.data.repository.common.impl

import com.gnoemes.shikimori.data.network.GraphqlApi
import com.gnoemes.shikimori.data.repository.common.TitleGenreSource
import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.data.graphql.GenreQueryResponse
import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlRequest
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TitleGenreSourceImpl @Inject constructor(
        private val api: GraphqlApi
) : TitleGenreSource {

    override fun genres(type: GenreEntryType, id: Long): Single<List<GenreV2>> =
            api.getGenres(GraphqlRequest(buildQuery(type, id)))
                    .map { convert(type, it) }
                    //see TitleGenreSource - a details screen must load with or without genres
                    .onErrorReturnItem(emptyList())

    /**
     * `limit` is mandatory: it defaults to 2 and would silently truncate a batched answer. Asking
     * by a single id makes that moot, but the default is a trap worth not stepping in twice - see
     * the same note in [PosterSourceImpl].
     *
     * `censored:false` is deliberate. With it true the query refuses to answer for hentai, yaoi
     * and yuri titles, and this runs for a title the user already has open - hiding its genres
     * would not hide anything they cannot already see. Whether such a title is reachable at all is
     * decided long before this call, by `allowR18Content` and the catalog's own `censored`.
     */
    private fun buildQuery(type: GenreEntryType, id: Long): String =
            "{${type.rootField}(ids:\"$id\",limit:1,censored:false)" +
                    "{id genres{id name russian kind}}}"

    private fun convert(type: GenreEntryType, response: GenreQueryResponse): List<GenreV2> {
        val holders = when (type) {
            GenreEntryType.ANIME -> response.data?.animes
            GenreEntryType.MANGA -> response.data?.mangas
        }

        return holders
                ?.firstOrNull()
                ?.genres
                .orEmpty()
                .mapNotNull { genre ->
                    //a genre with no id or no russian label cannot be shown or searched for, and
                    //shikimori declares both non-null - so this only fires if the api changes
                    val genreId = genre.id?.toLongOrNull() ?: return@mapNotNull null
                    val russian = genre.russian?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

                    GenreV2(
                            genreId,
                            genre.name.orEmpty(),
                            russian,
                            GenreV2.Kind.of(genre.kind)
                    )
                }
    }
}

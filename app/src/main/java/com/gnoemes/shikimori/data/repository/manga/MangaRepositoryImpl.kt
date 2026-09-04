package com.gnoemes.shikimori.data.repository.manga

import com.gnoemes.shikimori.data.local.db.MangaRateSyncDbSource
import com.gnoemes.shikimori.data.network.MangaApi
import com.gnoemes.shikimori.data.repository.common.FranchiseResponseConverter
import com.gnoemes.shikimori.data.repository.common.LinkResponseConverter
import com.gnoemes.shikimori.data.repository.common.MangaResponseConverter
import com.gnoemes.shikimori.data.repository.common.RolesResponseConverter
import com.gnoemes.shikimori.data.repository.manga.converter.MangaDetailsResponseConverter
import com.gnoemes.shikimori.entity.common.domain.Franchise
import com.gnoemes.shikimori.entity.common.domain.Link
import com.gnoemes.shikimori.entity.common.domain.Roles
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.manga.domain.MangaDetails
import com.gnoemes.shikimori.data.repository.common.TitleGenreSource
import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

class MangaRepositoryImpl @Inject constructor(
        private val api: MangaApi,
        private val syncDbSource: MangaRateSyncDbSource,
        private val detailsConverter: MangaDetailsResponseConverter,
        private val linkConverter: LinkResponseConverter,
        private val franchiseConverter: FranchiseResponseConverter,
        private val mangaConverter: MangaResponseConverter,
        private val rolesConverter: RolesResponseConverter,
        private val genreSource: TitleGenreSource
) : MangaRepository {

    /**
     * Genres come from graphql rather than from this rest response: only graphql carries the v2
     * taxonomy at all, and the rest response's own `genres` array is empty for titles added from
     * 2025 onward. The lookup never fails, so a details screen still loads when it comes back
     * empty - see docs/_internal/GENRES_V2_SPIKE.md.
     */
    override fun getDetails(id: Long): Single<MangaDetails> =
            Single.zip(
                    api.getDetails(id).map(detailsConverter),
                    genreSource.genres(GenreEntryType.MANGA, id),
                    BiFunction { details: MangaDetails, genres: List<GenreV2> ->
                        details.copy(genres = genres)
                    }
            ).flatMap { syncRate(it).toSingleDefault(it) }

    override fun getRoles(id: Long): Single<Roles> =
            api.getRoles(id)
                    .map(rolesConverter)

    override fun getLinks(id: Long): Single<List<Link>> =
            api.getLinks(id)
                    .map(linkConverter)

    override fun getSimilar(id: Long): Single<List<Manga>> =
            api.getSimilar(id)
                    .map(mangaConverter)

    override fun getFranchise(id: Long): Single<Franchise> =
            api.getFranchise(id)
                    .map(franchiseConverter)

    private fun syncRate(details: MangaDetails): Completable =
            Single.fromCallable { details }
                    .filter { details.userRate != null }
                    .flatMapCompletable { syncDbSource.saveRate(it.userRate!!) }
}
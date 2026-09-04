package com.gnoemes.shikimori.data.repository.anime

import com.gnoemes.shikimori.data.local.db.AnimeRateSyncDbSource
import com.gnoemes.shikimori.data.local.db.EpisodeDbSource
import com.gnoemes.shikimori.data.network.AnimeApi
import com.gnoemes.shikimori.data.repository.anime.converter.AnimeDetailsResponseConverter
import com.gnoemes.shikimori.data.repository.common.AnimeResponseConverter
import com.gnoemes.shikimori.data.repository.common.FranchiseResponseConverter
import com.gnoemes.shikimori.data.repository.common.LinkResponseConverter
import com.gnoemes.shikimori.data.repository.common.RolesResponseConverter
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.anime.domain.AnimeDetails
import com.gnoemes.shikimori.entity.anime.domain.Screenshot
import com.gnoemes.shikimori.entity.common.domain.Franchise
import com.gnoemes.shikimori.entity.common.domain.Link
import com.gnoemes.shikimori.entity.common.domain.Roles
import com.gnoemes.shikimori.utils.appendHostIfNeed
import com.gnoemes.shikimori.data.repository.common.TitleGenreSource
import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

class AnimeRepositoryImpl @Inject constructor(
        private val api: AnimeApi,
        private val syncDbSource: AnimeRateSyncDbSource,
        private val episodeDbSource: EpisodeDbSource,
        private val linkConverter: LinkResponseConverter,
        private val animeConverter: AnimeResponseConverter,
        private val franchiseConverter: FranchiseResponseConverter,
        private val detailsConverter: AnimeDetailsResponseConverter,
        private val rolesConverter: RolesResponseConverter,
        private val genreSource: TitleGenreSource
) : AnimeRepository {

    /**
     * Genres come from graphql rather than from this rest response: only graphql carries the v2
     * taxonomy at all, and the rest response's own `genres` array is empty for titles added from
     * 2025 onward. The lookup never fails, so a details screen still loads when it comes back
     * empty - see docs/_internal/GENRES_V2_SPIKE.md.
     */
    override fun getDetails(id: Long): Single<AnimeDetails> =
            Single.zip(
                    api.getDetails(id).map(detailsConverter),
                    genreSource.genres(GenreEntryType.ANIME, id),
                    BiFunction { details: AnimeDetails, genres: List<GenreV2> ->
                        details.copy(genres = genres)
                    }
            ).flatMap { syncRate(it).toSingleDefault(it) }

    override fun getRoles(id: Long): Single<Roles> =
            api.getRoles(id)
                    .map(rolesConverter)

    override fun getLinks(id: Long): Single<List<Link>> =
            api.getLinks(id)
                    .map(linkConverter)

    override fun getSimilar(id: Long): Single<List<Anime>> =
            api.getSimilar(id)
                    .map(animeConverter)

    override fun getFranchise(id: Long): Single<Franchise> =
            api.getFranchise(id)
                    .map(franchiseConverter)

    override fun getScreenshots(id: Long): Single<List<Screenshot>> =
            api.getScreenshots(id)
                    .map { list -> list.map { Screenshot(it.original?.appendHostIfNeed(), it.preview?.appendHostIfNeed()) } }

    override fun getLocalWatchedAnimeIds(): Single<LinkedHashSet<Long>> =
            episodeDbSource.getWatchedAnimeIds()
                    .map { LinkedHashSet(it) }

    private fun syncRate(details: AnimeDetails): Completable =
            Single.fromCallable { details }
                    .filter { details.userRate?.targetId != null && details.userRate.episodes != null }
                    .flatMapCompletable { syncDbSource.saveRate(it.userRate!!) }

}
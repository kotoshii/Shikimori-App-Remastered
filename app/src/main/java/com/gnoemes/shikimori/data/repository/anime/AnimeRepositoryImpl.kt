package com.gnoemes.shikimori.data.repository.anime

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
import io.reactivex.Single
import javax.inject.Inject

class AnimeRepositoryImpl @Inject constructor(
        private val api: AnimeApi,
        private val linkConverter: LinkResponseConverter,
        private val animeConverter: AnimeResponseConverter,
        private val franchiseConverter: FranchiseResponseConverter,
        private val detailsConverter: AnimeDetailsResponseConverter,
        private val rolesConverter: RolesResponseConverter,
        private val genreSource: TitleGenreSource
) : AnimeRepository {

    override fun getDetails(id: Long): Single<AnimeDetails> =
            genreSource.withGenres(api.getDetails(id).map(detailsConverter), GenreEntryType.ANIME, id) { details, genres ->
                details.copy(genres = genres)
            }

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

}
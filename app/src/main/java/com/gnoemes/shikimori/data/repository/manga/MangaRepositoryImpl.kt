package com.gnoemes.shikimori.data.repository.manga

import com.gnoemes.shikimori.data.network.MangaApi
import com.gnoemes.shikimori.data.repository.common.FranchiseResponseConverter
import com.gnoemes.shikimori.data.repository.common.LinkResponseConverter
import com.gnoemes.shikimori.data.repository.common.MangaResponseConverter
import com.gnoemes.shikimori.data.repository.common.RolesResponseConverter
import com.gnoemes.shikimori.data.repository.common.TitleGenreSource
import com.gnoemes.shikimori.data.repository.manga.converter.MangaDetailsResponseConverter
import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.domain.Franchise
import com.gnoemes.shikimori.entity.common.domain.Link
import com.gnoemes.shikimori.entity.common.domain.Roles
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.manga.domain.MangaDetails
import io.reactivex.Single
import javax.inject.Inject

class MangaRepositoryImpl @Inject constructor(
        private val api: MangaApi,
        private val detailsConverter: MangaDetailsResponseConverter,
        private val linkConverter: LinkResponseConverter,
        private val franchiseConverter: FranchiseResponseConverter,
        private val mangaConverter: MangaResponseConverter,
        private val rolesConverter: RolesResponseConverter,
        private val genreSource: TitleGenreSource
) : MangaRepository {

    //a ranobe's genres are manga genres to graphql too
    override fun getDetails(id: Long, type: Type): Single<MangaDetails> =
            genreSource.withGenres(api.getDetails(MangaApi.kind(type), id).map(detailsConverter), GenreEntryType.MANGA, id) { details, genres ->
                details.copy(genres = genres)
            }

    override fun getRoles(id: Long, type: Type): Single<Roles> =
            api.getRoles(MangaApi.kind(type), id)
                    .map(rolesConverter)

    override fun getLinks(id: Long, type: Type): Single<List<Link>> =
            api.getLinks(MangaApi.kind(type), id)
                    .map(linkConverter)

    override fun getSimilar(id: Long, type: Type): Single<List<Manga>> =
            api.getSimilar(MangaApi.kind(type), id)
                    .map(mangaConverter)

    override fun getFranchise(id: Long, type: Type): Single<Franchise> =
            api.getFranchise(MangaApi.kind(type), id)
                    .map(franchiseConverter)
}

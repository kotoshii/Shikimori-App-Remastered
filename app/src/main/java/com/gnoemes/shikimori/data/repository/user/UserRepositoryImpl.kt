package com.gnoemes.shikimori.data.repository.user

import com.gnoemes.shikimori.data.local.preference.UserSource
import com.gnoemes.shikimori.data.network.UserApi
import com.gnoemes.shikimori.data.repository.club.ClubResponseConverter
import com.gnoemes.shikimori.data.repository.user.converter.*
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.club.domain.Club
import com.gnoemes.shikimori.entity.user.domain.*
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
        private val api: UserApi,
        private val userSource: UserSource,
        private val converter: UserBriefResponseConverter,
        private val detailsConverter: UserDetailsResponseConverter,
        private val historyConverter: UserHistoryConverter,
        private val favoriteConverter: FavoriteListResponseConverter,
        private val clubConverter: ClubResponseConverter
) : UserRepository {

    override fun getMyUserId(): Single<Long> =
            Single.just(userSource.getUserId())
                    .flatMap {
                        if (it == Constants.NO_ID) getMyUserBrief().map { it.id }
                        else Single.just(it)
                    }

    //the saved user while signed in, else from the api - also when the saved one cannot be read
    override fun getMyUserBrief(): Single<UserBrief> =
            Maybe.fromCallable<UserBrief> { if (userSource.getUserStatus() == UserStatus.AUTHORIZED) userSource.getUser() else null }
                    .switchIfEmpty(api.getCurrentUserBrief()
                            .map { converter.convertResponse(it)!! }
                            .doOnSuccess { userSource.setUser(it) }
                            .doOnSuccess { userSource.setUserId(it.id) })

    override fun getDetails(id: Long): Single<UserDetails> =
            api.getUserProfile(id)
                    .map(detailsConverter::convertResponse)

    override fun getFriends(id: Long, page: Int, limit: Int): Single<Page<UserBrief>> =
            api.getUserFriends(id, page, limit)
                    .map { Page.exact(it, limit) }
                    .map { it.map(converter::apply) }

    override fun getFavorites(id: Long): Single<FavoriteList> =
            api.getUserFavourites(id)
                    .map(favoriteConverter)

    override fun getClubs(id: Long): Single<List<Club>> =
            api.getUserClubs(id)
                    .map(clubConverter)

    override fun getHistory(id: Long, page: Int, limit: Int): Single<Page<UserHistory>> =
            api.getUserHistory(id, page, limit)
                    .map { Page.withExtraRow(it, limit) }
                    .map { it.map(historyConverter::apply) }

    override fun ignore(id: Long): Completable = api.ignoreUser(id)

    override fun unignore(id: Long): Completable = api.unignoreUser(id)

    override fun addToFriends(id: Long): Completable = api.addToFriends(id)

    override fun removeFriend(id: Long): Completable = api.deleteFriend(id)

    override fun getUserStatus(): UserStatus = userSource.getUserStatus()

    override fun setUserStatus(status: UserStatus) = userSource.setUserStatus(status)
}
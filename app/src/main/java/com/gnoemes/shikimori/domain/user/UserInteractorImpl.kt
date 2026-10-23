package com.gnoemes.shikimori.domain.user

import com.gnoemes.shikimori.data.repository.user.UserRepository
import com.gnoemes.shikimori.entity.club.domain.Club
import com.gnoemes.shikimori.entity.user.domain.*
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import com.gnoemes.shikimori.utils.network.AuthHolder
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class UserInteractorImpl @Inject constructor(
        private val repository: UserRepository,
        //owns the login (P7-T5 stage 1): the same sign-out as a refused refresh token
        private val authHolder: AuthHolder
) : UserInteractor {

    override fun getMyUserId(): Single<Long> = repository.getMyUserId().applyErrorHandlerAndSchedulers()

    override fun getDetails(id: Long): Single<UserDetails> =
            repository.getDetails(id)
                    .flatMap { detals ->
                        repository.getMyUserId()
                                .map { detals.copy(isMe = detals.id == it) }
                                .onErrorReturn { detals.copy(isMe = false) }
                    }
                    .applyErrorHandlerAndSchedulers()

    override fun getFriends(id: Long, page: Int, limit: Int): Single<List<UserBrief>> = repository.getFriends(id, page, limit).applyErrorHandlerAndSchedulers()

    override fun getFavorites(id: Long): Single<FavoriteList> = repository.getFavorites(id).applyErrorHandlerAndSchedulers()

    override fun getClubs(id: Long): Single<List<Club>> = repository.getClubs(id).applyErrorHandlerAndSchedulers()

    override fun getHistory(id: Long, page: Int, limit: Int): Single<List<UserHistory>> = repository.getHistory(id, page, limit).applyErrorHandlerAndSchedulers()

    override fun ignore(id: Long): Completable = repository.ignore(id).applyErrorHandlerAndSchedulers()

    override fun unignore(id: Long): Completable = repository.unignore(id).applyErrorHandlerAndSchedulers()

    override fun addToFriends(id: Long): Completable = repository.addToFriends(id).applyErrorHandlerAndSchedulers()

    override fun removeFriend(id: Long): Completable = repository.removeFriend(id).applyErrorHandlerAndSchedulers()

    override fun getUserStatus(): UserStatus = repository.getUserStatus()

    override fun signOut(): Completable =
            Completable.fromAction { authHolder.signOut() }
                    .applyErrorHandlerAndSchedulers()
}
package com.gnoemes.shikimori.domain.auth

import com.gnoemes.shikimori.data.repository.app.AuthorizationRepository
import com.gnoemes.shikimori.data.repository.app.TokenRepository
import com.gnoemes.shikimori.data.repository.user.UserRepository
import com.gnoemes.shikimori.entity.user.domain.UserStatus
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import com.gnoemes.shikimori.utils.network.AuthHolder
import io.reactivex.Completable
import javax.inject.Inject

class AuthInteractorImpl @Inject constructor(
        private val tokenRepository: TokenRepository,
        private val userRepository: UserRepository,
        private val repository: AuthorizationRepository,
        private val authHolder: AuthHolder
) : AuthInteractor {

    override fun signIn(authCode: String): Completable =
            repository.signIn(authCode)
                    .flatMapCompletable { tokenRepository.saveToken(it) }
                    .andThen(userRepository.getMyUserBrief())
                    .flatMapCompletable { Completable.fromAction { userRepository.setUserStatus(UserStatus.AUTHORIZED) } }
                    //the token was saved before the user loaded: a failure after it left a half sign-in, a
                    //token while the app counted the user a guest (BUGS #22)
                    .doOnError { authHolder.forgetLogin() }
                    .applyErrorHandlerAndSchedulers()
}
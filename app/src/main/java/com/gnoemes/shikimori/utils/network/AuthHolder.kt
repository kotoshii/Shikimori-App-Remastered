package com.gnoemes.shikimori.utils.network

import android.annotation.SuppressLint
import com.gnoemes.shikimori.data.local.preference.UserSource
import com.gnoemes.shikimori.data.repository.app.AuthorizationRepository
import com.gnoemes.shikimori.data.repository.app.TokenRepository
import com.gnoemes.shikimori.entity.app.domain.Token
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.HttpException
import javax.inject.Inject

class AuthHolder @Inject constructor(
        private val tokenRepository: TokenRepository,
        private val authRepository: AuthorizationRepository,
        private val userRepository: UserSource,
        private val siteCookies: ShikimoriSiteCookies
) {

    fun getToken(): Token? = tokenRepository.getToken()

    private fun updateToken(): Completable {
        val token = getToken()

        if (token == null || token.refreshToken.isEmpty()) {
            return Completable.fromAction { userRepository.clearUser() }
        }

        return Single.fromCallable { token }
                .flatMap { authRepository.refreshToken(it.refreshToken) }
                .flatMapCompletable { tokenRepository.saveToken(it) }
    }

    @SuppressLint("CheckResult")
    fun refresh() {
        updateToken()
                .subscribe({}, {
                    if (it is HttpException) signOut()
                })

    }

    /** Forgets the login on the phone, and the site's session in the in-app browser (BUGS #4). */
    fun signOut() {
        userRepository.clearUser()
        tokenRepository.saveToken(null).blockingAwait()
        siteCookies.forget()
    }

}
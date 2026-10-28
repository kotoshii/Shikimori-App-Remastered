package com.gnoemes.shikimori.utils.network

import android.util.Log
import com.gnoemes.shikimori.data.local.preference.UserSource
import com.gnoemes.shikimori.data.repository.app.AuthorizationRepository
import com.gnoemes.shikimori.data.repository.app.TokenRepository
import com.gnoemes.shikimori.entity.app.domain.Token
import java.io.IOException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** The header the signed-in client sends its token in. */
const val AUTHORIZATION_HEADER = "Authorization"

/** That header's value for this token: one spelling for the request, the refresh and the retry. */
val Token.authorizationHeader: String
    get() = "Bearer $authToken"

private const val HTTP_BAD_REQUEST = 400
private const val HTTP_UNAUTHORIZED = 401

/**
 * A request during which the app signed the user out - shikimori refused the login, or the saved one
 * could not be read - or that was sent signed in just before another request did. The screen says so
 * instead of showing the 401 (BUGS #70).
 */
class SignedOutException : IOException("signed out")

/**
 * The login's one owner: the token to send, its refresh, and signing out.
 *
 * One instance for the app, so that one refresh runs at a time: a screen sends several requests at
 * once, they all get a 401 when the token expires, and each used to refresh with the same refresh
 * token - the one that lost could sign the user out (BUGS #35).
 */
@Singleton
class AuthHolder @Inject constructor(
        private val tokenRepository: TokenRepository,
        private val authRepository: AuthorizationRepository,
        private val userRepository: UserSource,
        private val siteCookies: ShikimoriSiteCookies
) {

    companion object {
        private const val TAG = "AuthHolder"
    }

    /**
     * The token to send, or null for a guest. Signed in without a token that can be read means the
     * saved login is broken - by an update that renamed its fields in a release build, say - so the
     * user is signed out rather than left sending requests that all fail (BUGS #21), and this request
     * fails with [SignedOutException].
     */
    @Throws(SignedOutException::class)
    fun getToken(): Token? {
        val token = tokenRepository.getToken()
        if (token == null && userRepository.isSignedIn()) {
            signOut()
            throw SignedOutException()
        }
        return token
    }

    /**
     * A fresh token after a request sent with [sentHeader] was refused, or null to give up and keep
     * the login: a network or server error, and the next request tries again. A request that raced
     * another one's refresh gets the token that refresh saved.
     *
     * Throws [SignedOutException] when shikimori refused the refresh token - the user is signed out
     * here - or when another request already signed the user out.
     */
    @Synchronized
    @Throws(SignedOutException::class)
    fun refreshAfter(sentHeader: String): Token? {
        val stored = tokenRepository.getToken() ?: throw SignedOutException()
        if (stored.authorizationHeader != sentHeader) return stored

        try {
            return authRepository.refreshToken(stored.refreshToken)
                    .flatMap { tokenRepository.saveToken(it).toSingleDefault(it) }
                    .blockingGet()
        } catch (e: Exception) {
            Log.w(TAG, "token refresh failed", e)
            val code = (e as? HttpException)?.code()
            if (code != HTTP_BAD_REQUEST && code != HTTP_UNAUTHORIZED) return null
        }
        signOut()
        throw SignedOutException()
    }

    /** Forgets the login on the phone, and the site's session in the in-app browser (BUGS #4). */
    @Synchronized
    fun signOut() {
        forgetLogin()
        siteCookies.forget()
    }

    /**
     * Forgets the login on the phone only - the user and the tokens. A sign-in that fails half way
     * undoes just this: the site session it opened in the in-app browser stays (BUGS #22).
     */
    @Synchronized
    fun forgetLogin() {
        userRepository.clearUser()
        tokenRepository.saveToken(null).blockingAwait()
    }

}
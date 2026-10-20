package com.gnoemes.shikimori.utils.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Adds the user's token to every request of the signed-in client; a guest's go without one. A saved
 * login that cannot be read fails the request with [SignedOutException] (from [AuthHolder.getToken]).
 */
class TokenInterceptor @Inject constructor(
        private val holder: AuthHolder
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = holder.getToken() ?: return chain.proceed(chain.request())

        return chain.proceed(chain.request().newBuilder()
                .addHeader(AUTHORIZATION_HEADER, token.authorizationHeader)
                .build())
    }
}
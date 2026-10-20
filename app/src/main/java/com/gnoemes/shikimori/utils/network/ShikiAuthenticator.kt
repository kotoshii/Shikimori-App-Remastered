package com.gnoemes.shikimori.utils.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * Answers a 401 from the signed-in client: one retry on a fresh token, or none. Every 401 used to be
 * retried - up to OkHttp's 20 follow-ups, past the rate limiter - so a login that no longer worked
 * sent 21 requests for each one a screen made (BUGS #35).
 */
class ShikiAuthenticator @Inject constructor(
        private val holder: AuthHolder
) : Authenticator {

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        //a guest's request has no token to refresh, and one already retried on a fresh token was
        //refused for some other reason
        val sent = response.request().header(AUTHORIZATION_HEADER) ?: return null
        if (generateSequence(response.priorResponse()) { it.priorResponse() }.any { it.code() == HTTP_UNAUTHORIZED }) return null

        val token = holder.refreshAfter(sent) ?: return null
        return response.request().newBuilder()
                .header(AUTHORIZATION_HEADER, token.authorizationHeader)
                .build()
    }
}
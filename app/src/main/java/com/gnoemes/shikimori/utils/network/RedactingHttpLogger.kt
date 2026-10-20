package com.gnoemes.shikimori.utils.network

import okhttp3.logging.HttpLoggingInterceptor

/**
 * The request log of debug builds, with the secrets masked: the login and Anime365 tokens, the OAuth
 * secret and code, Kodik's token and the cookies. The log can be read over USB, and it ends up in bug
 * reports and in session notes (BUGS #5). Release builds log nothing at all (RetrofitModule).
 */
class RedactingHttpLogger : HttpLoggingInterceptor.Logger {

    override fun log(message: String) = HttpLoggingInterceptor.Logger.DEFAULT.log(redact(message))

    companion object {
        private const val MASK = "***"

        //OkHttp logs one header per line; a body comes whole, so the other two may match anywhere in it
        private val HEADER = Regex("""^(Authorization|Cookie|Set-Cookie):.*""", RegexOption.IGNORE_CASE)
        //in a url's query, or in a form body, which starts without the "?"
        private val PARAMETER = Regex("""((?:^|[?&])(?:access_token|refresh_token|client_secret|code|token)=)[^&#\s]*""")
        private val JSON_FIELD = Regex("""("(?:access_token|refresh_token)"\s*:\s*")[^"]*""")

        fun redact(message: String): String = message
                .replace(HEADER) { "${it.groupValues[1]}: $MASK" }
                .replace(PARAMETER) { "${it.groupValues[1]}$MASK" }
                .replace(JSON_FIELD) { "${it.groupValues[1]}$MASK" }
    }
}

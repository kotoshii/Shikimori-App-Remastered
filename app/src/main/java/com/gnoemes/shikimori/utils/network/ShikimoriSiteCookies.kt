package com.gnoemes.shikimori.utils.network

import android.util.Log
import android.webkit.CookieManager
import com.gnoemes.shikimori.BuildConfig
import com.gnoemes.shikimori.utils.toUri
import javax.inject.Inject

/**
 * The shikimori site's session in the in-app browser's cookie store - what the sign-in page leaves
 * behind, valid for years. Without forgetting it, the next «Войти» signs the same account straight
 * back in, so there is no way to switch accounts (BUGS #4). Only shikimori's cookies go: «Веб-плеер»
 * logins, the Anime365 site and Mail.ru's video cookie stay.
 *
 * Android has no way to delete one site's cookies, so each is overwritten with an expired copy, which
 * deletes it. The store gives names only, not whether a cookie belongs to the host alone or to
 * `.host` and its subdomains, so both forms are expired. The names are read, not listed here: the site
 * is moving to new software, its old pages still set `_kawai_session` but the sign-in page is a new one
 * with cookies of its own, and which cookie holds the login after a sign-in is not known.
 */
class ShikimoriSiteCookies @Inject constructor() {

    companion object {
        private const val TAG = "SiteCookies"
        private const val EXPIRED = "Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; Secure"

        //the app signed in on these before shikimori.io, so an install that ran an older build may
        //still hold their cookies
        private val OLD_HOSTS = listOf("shikimori.one", "shikimori.me", "shikimori.org")
    }

    fun forget() {
        val cookies = CookieManager.getInstance()
        val hosts = listOfNotNull(BuildConfig.ShikimoriBaseUrl.toUri().host) + OLD_HOSTS

        hosts.forEach { host ->
            val url = "https://$host/"
            val names = cookies.getCookie(url)
                    ?.split(";")
                    ?.map { it.substringBefore("=").trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty()

            names.forEach { name ->
                cookies.setCookie(url, "$name=; $EXPIRED")
                cookies.setCookie(url, "$name=; $EXPIRED; Domain=$host")
            }
            if (names.isNotEmpty()) Log.i(TAG, "expired ${names.size} cookies of $host")
        }
        cookies.flush()
    }
}

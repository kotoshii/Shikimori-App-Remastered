package com.gnoemes.shikimori.data.repository.series.smotretanime

/** The Anime365 sign-in token; every reader and writer goes through here, whatever stores it. */
interface Anime365TokenSource  {

    fun getToken() : String?

    fun setToken(token: String)

    fun clearToken()
}

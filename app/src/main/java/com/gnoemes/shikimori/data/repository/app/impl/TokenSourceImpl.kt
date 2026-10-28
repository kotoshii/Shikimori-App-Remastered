package com.gnoemes.shikimori.data.repository.app.impl

import android.content.SharedPreferences
import com.gnoemes.shikimori.data.repository.app.TokenSource
import com.gnoemes.shikimori.di.app.annotations.UserQualifier
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.app.domain.Token
import com.gnoemes.shikimori.utils.putString
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.reactivex.Completable
import javax.inject.Inject

class TokenSourceImpl @Inject constructor(
        @UserQualifier private val preferences: SharedPreferences,
        private val gson: Gson
) : TokenSource {

    override fun saveToken(token: Token?): Completable = Completable.fromAction {
        val json = gson.toJson(token)
        val value = if (json == "null") null else json
        preferences.putString(AppExtras.ARGUMENT_TOKEN, value)
    }

    /**
     * The saved token, or null - also for a value that cannot be read, or reads with empty fields:
     * Gson leaves a field empty when its stored name no longer matches, as after a release build's
     * shrinking renames it (BUGS #21).
     */
    override fun getToken(): Token? {
        val json = preferences.getString(AppExtras.ARGUMENT_TOKEN, null) ?: return null
        val token = try {
            gson.fromJson<Token>(json, Token::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
        return token?.takeUnless { it.authToken.isNullOrBlank() || it.refreshToken.isNullOrBlank() }
    }
}
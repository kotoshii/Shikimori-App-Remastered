package com.gnoemes.shikimori.data.repository.series.smotretanime

import android.content.SharedPreferences
import com.gnoemes.shikimori.di.app.annotations.UserQualifier
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.utils.putString
import com.gnoemes.shikimori.utils.remove
import javax.inject.Inject

class Anime365TokenSourceImpl @Inject constructor(
        @UserQualifier private val prefs: SharedPreferences
) : Anime365TokenSource {

    override fun getToken(): String? = prefs.getString(SettingsExtras.ANIME_365_TOKEN, null)

    override fun setToken(token: String) = prefs.putString(SettingsExtras.ANIME_365_TOKEN, token)

    override fun clearToken() = prefs.remove(SettingsExtras.ANIME_365_TOKEN)
}
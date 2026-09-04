package com.gnoemes.shikimori.data.local.preference.impl

import android.content.SharedPreferences
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.UserSource
import com.gnoemes.shikimori.di.app.annotations.SettingsQualifier
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.entity.chronology.ChronologyType
import com.gnoemes.shikimori.entity.rates.domain.RateSwipeAction
import com.gnoemes.shikimori.entity.series.domain.PlayerType
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import com.gnoemes.shikimori.utils.putBoolean
import com.gnoemes.shikimori.utils.putInt
import com.gnoemes.shikimori.utils.putString
import com.gnoemes.shikimori.utils.putStringSet
import javax.inject.Inject

class SettingsSourceImpl @Inject constructor(
        @SettingsQualifier private val prefs: SharedPreferences,
        private val userSource: UserSource
) : SettingsSource {

    override var isAutoStatus: Boolean
        get() = prefs.getBoolean(SettingsExtras.IS_AUTO_STATUS, true)
        set(value) = prefs.putBoolean(SettingsExtras.IS_AUTO_STATUS, value)

    override var isAutoIncrement: Boolean
        get() = prefs.getBoolean(SettingsExtras.IS_AUTO_INCREMENT, true)
        set(value) = prefs.putBoolean(SettingsExtras.IS_AUTO_INCREMENT, value)

    override var isRussianNaming: Boolean
        get() = prefs.getBoolean(SettingsExtras.IS_ROMADZI_NAMING, true)
        set(value) = prefs.putBoolean(SettingsExtras.IS_ROMADZI_NAMING, value)

    /**
     * Reports false whenever nobody is signed in, whatever is stored.
     *
     * Upstream only hides the toggle when logged out, which leaves a stored `true` in force - a user
     * who enabled r18 and then signed out kept seeing r18 content. Gating the **value** instead
     * covers all three readers at once - `SearchQueryBuilderImpl`, `FilterSourceImpl` and
     * `PosterSourceImpl` - rather than relying on each of them to remember to check.
     *
     * The stored choice is deliberately left alone by the setter, so signing back in restores it.
     * `PosterSourceImpl` compares this value against the one its cache was built with, so signing
     * out invalidates the poster cache on its own.
     */
    override var allowR18Content: Boolean
        get() = userSource.getUserId() != Constants.NO_ID && prefs.getBoolean(SettingsExtras.ALLOW_R18_CONTENT, false)
        set(value) = prefs.putBoolean(SettingsExtras.ALLOW_R18_CONTENT, value)

    override var altSourceByDefault: Boolean
        get() = prefs.getBoolean(SettingsExtras.ALT_SOURCE_BY_DEFAULT, false)
        set(value) = prefs.putBoolean(SettingsExtras.ALT_SOURCE_BY_DEFAULT, value)

    override var isAskForPlayer: Boolean
        get() = prefs.getBoolean(SettingsExtras.IS_REMEMBER_PLAYER, true)
        set(value) = prefs.putBoolean(SettingsExtras.IS_REMEMBER_PLAYER, value)

    override var isNotificationsEnabled: Boolean
        get() = prefs.getBoolean(SettingsExtras.IS_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.putBoolean(SettingsExtras.IS_NOTIFICATIONS_ENABLED, value)

    override var translationType: TranslationType
        get() {
            val type = prefs.getString(SettingsExtras.TRANSLATION_TYPE, "")
            return TranslationType.values().find { it.isEqualType(type) }
                    ?: TranslationType.VOICE_RU
        }
        set(value) = prefs.putString(SettingsExtras.TRANSLATION_TYPE, value.type)

    override var playerType: PlayerType
        get() {
            val type = prefs.getString(SettingsExtras.PLAYER_TYPE, PlayerType.EMBEDDED.name)!!
            return PlayerType.valueOf(type)
        }
        set(value) = prefs.putString(SettingsExtras.PLAYER_TYPE, value.name)

    override var useLocalTranslationSettings: Boolean
        get() = prefs.getBoolean(SettingsExtras.IS_USE_LOCAL_TRANSLATION_SETTINGS, true)
        set(value) = prefs.putBoolean(SettingsExtras.IS_USE_LOCAL_TRANSLATION_SETTINGS, value)

    override var downloadFolder: String
        get() = prefs.getString(SettingsExtras.DOWNLOAD_FOLDER, "")!!
        set(value) = prefs.putString(SettingsExtras.DOWNLOAD_FOLDER, value)

    override var isExternalBestQuality: Boolean
        get() = prefs.getBoolean(SettingsExtras.IS_BEST_EXTERNAL_QUALITY, false)
        set(value) = prefs.putBoolean(SettingsExtras.IS_BEST_EXTERNAL_QUALITY, value)

    override var rateSwipeToLeftAction: RateSwipeAction
        get() = prefs.getString(SettingsExtras.RATE_SWIPE_TO_LEFT_ACTION, RateSwipeAction.INCREMENT.name)?.let { action ->
            RateSwipeAction.values().find { it.name == action } ?: RateSwipeAction.INCREMENT
        } ?: RateSwipeAction.INCREMENT
        set(value) = prefs.putString(SettingsExtras.RATE_SWIPE_TO_LEFT_ACTION, value.name)

    override var rateSwipeToRightAction: RateSwipeAction
        get() = prefs.getString(SettingsExtras.RATE_SWIPE_TO_RIGHT_ACTION, RateSwipeAction.CHANGE.name)?.let { action ->
            RateSwipeAction.values().find { it.name == action } ?: RateSwipeAction.CHANGE
        } ?: RateSwipeAction.CHANGE
        set(value) = prefs.putString(SettingsExtras.RATE_SWIPE_TO_RIGHT_ACTION, value.name)

    override var chronologyType: ChronologyType
        get() = prefs.getInt(SettingsExtras.CHRONOLOGY_TYPE, ChronologyType.MAIN.ordinal).let { type ->
            ChronologyType.values().find { it.ordinal == type } ?: ChronologyType.MAIN
        }
        set(value) = prefs.putInt(SettingsExtras.CHRONOLOGY_TYPE, value.ordinal)

    override var seenHostings: Set<String>
        get() = prefs.getStringSet(SettingsExtras.SEEN_HOSTINGS, emptySet()).orEmpty()
        //a copy, because SharedPreferences must not be handed a set it still holds a reference to
        set(value) = prefs.putStringSet(SettingsExtras.SEEN_HOSTINGS, LinkedHashSet(value))

    override var hiddenHostings: Set<String>
        get() = prefs.getStringSet(SettingsExtras.HIDDEN_HOSTINGS, emptySet()).orEmpty()
        set(value) = prefs.putStringSet(SettingsExtras.HIDDEN_HOSTINGS, LinkedHashSet(value))

    override var animeGenres: Set<String>
        get() = prefs.getStringSet(SettingsExtras.ANIME_GENRES_V2, emptySet()).orEmpty()
        set(value) = prefs.putStringSet(SettingsExtras.ANIME_GENRES_V2, LinkedHashSet(value))

    override var mangaGenres: Set<String>
        get() = prefs.getStringSet(SettingsExtras.MANGA_GENRES_V2, emptySet()).orEmpty()
        set(value) = prefs.putStringSet(SettingsExtras.MANGA_GENRES_V2, LinkedHashSet(value))

    override var hideAnime365: Boolean
        get() = prefs.getBoolean(SettingsExtras.HIDE_ANIME_365, false)
        set(value) = prefs.putBoolean(SettingsExtras.HIDE_ANIME_365, value)
}
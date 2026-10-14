package com.gnoemes.shikimori.data.local.preference

import android.content.Context
import android.content.SharedPreferences
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.entity.app.domain.ThemeExtras
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.utils.getDefaultSharedPreferences
import com.gnoemes.shikimori.utils.getThemeSharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

/**
 * The settings backup file: what it holds and how it is read back. [KEYS] is the one list - a key
 * is saved and restored only if it is here, so a new setting is added here once, and caches, the
 * account and the Anime365 token never reach the file.
 *
 * Restoring an older file still works: it holds more keys (caches and so on), and those are skipped.
 */
class SettingsBackup @Inject constructor(
        private val context: Context
) {

    private enum class Kind { BOOLEAN, INT, LONG, STRING, STRING_SET }

    private class Key(val name: String, val kind: Kind, val isTheme: Boolean = false)

    companion object {
        private val KEYS = listOf(
                Key(ThemeExtras.THEME_KEY, Kind.INT, isTheme = true),
                Key(ThemeExtras.ASCENT_KEY, Kind.INT, isTheme = true),
                Key(ThemeExtras.NIGHT_THEME_KEY, Kind.INT, isTheme = true),
                Key(ThemeExtras.NIGHT_THEME_START_KEY, Kind.INT, isTheme = true),
                Key(ThemeExtras.NIGHT_THEME_END_KEY, Kind.INT, isTheme = true),

                Key(SettingsExtras.IS_ROMADZI_NAMING, Kind.BOOLEAN),
                Key(SettingsExtras.ALLOW_R18_CONTENT, Kind.BOOLEAN),
                Key(SettingsExtras.ALT_SOURCE_BY_DEFAULT, Kind.BOOLEAN),
                Key(SettingsExtras.DOWNLOAD_FOLDER, Kind.STRING),
                Key(SettingsExtras.IS_AUTO_INCREMENT, Kind.BOOLEAN),
                Key(SettingsExtras.IS_AUTO_STATUS, Kind.BOOLEAN),
                Key(SettingsExtras.TRANSLATION_TYPE, Kind.STRING),
                Key(SettingsExtras.PLAYER_TYPE, Kind.STRING),
                Key(SettingsExtras.IS_REMEMBER_PLAYER, Kind.BOOLEAN),
                Key(SettingsExtras.IS_USE_LOCAL_TRANSLATION_SETTINGS, Kind.BOOLEAN),
                Key(SettingsExtras.IS_BEST_EXTERNAL_QUALITY, Kind.BOOLEAN),
                Key(SettingsExtras.RATE_SWIPE_TO_LEFT_ACTION, Kind.STRING),
                Key(SettingsExtras.RATE_SWIPE_TO_RIGHT_ACTION, Kind.STRING),
                Key(SettingsExtras.HIDE_ANIME_365, Kind.BOOLEAN),
                Key(SettingsExtras.CHECK_UPDATES_ON_START, Kind.BOOLEAN),
                Key(SettingsExtras.MANUAL_HOSTINGS, Kind.STRING_SET),
                Key(SettingsExtras.HIDDEN_HOSTINGS, Kind.STRING_SET),

                Key(SettingsExtras.PLAYER_IS_GESTURES_ENABLED, Kind.BOOLEAN),
                Key(SettingsExtras.PLAYER_IS_VOLUME_BRIGHTNESS_GESTURES_ENABLED, Kind.BOOLEAN),
                Key(SettingsExtras.PLAYER_IS_VOLUME_AND_BRIGHTNESS_INVERTED, Kind.BOOLEAN),
                Key(SettingsExtras.PLAYER_IS_FORWARD_REWIND_SLIDE, Kind.BOOLEAN),
                Key(SettingsExtras.PLAYER_IS_OPEN_LANDSCAPE, Kind.BOOLEAN),
                Key(SettingsExtras.PLAYER_FORWARD_REWIND_OFFSET, Kind.LONG),
                Key(SettingsExtras.PLAYER_FORWARD_REWIND_OFFSET_BIG, Kind.LONG),
                Key(SettingsExtras.PLAYER_IS_ZOOM_PROPORTIONAL, Kind.BOOLEAN),
                Key(SettingsExtras.PLAYER_IS_AUTO_PIP, Kind.BOOLEAN),

                Key(SettingsExtras.CHRONOLOGY_TYPE, Kind.INT),
                Key(SettingsExtras.SORT_KEY_PART + "_${Type.ANIME.name}", Kind.INT),
                Key(SettingsExtras.SORT_KEY_PART + "_${Type.MANGA.name}", Kind.INT),
                Key(SettingsExtras.ORDER_KEY_PART + "_${Type.ANIME.name}", Kind.BOOLEAN),
                Key(SettingsExtras.ORDER_KEY_PART + "_${Type.MANGA.name}", Kind.BOOLEAN)
        )
    }

    private val prefs: SharedPreferences
        get() = context.getDefaultSharedPreferences()

    private val themePrefs: SharedPreferences
        get() = context.getThemeSharedPreferences()

    /** The settings that are set, as the file's JSON. */
    fun toJson(): String {
        val stored = prefs.all
        val storedTheme = themePrefs.all
        val values = KEYS
                .mapNotNull { key -> (if (key.isTheme) storedTheme else stored)[key.name]?.let { key.name to it } }
                .toMap()
        return Gson().toJson(values)
    }

    /** Restores every known key the JSON holds with the right type; returns how many. */
    fun restore(json: String): Int {
        val mapType = object : TypeToken<Map<String, Any?>>() {}.type
        val gson = GsonBuilder().registerTypeAdapter(mapType, MapDeserializerDoubleAsIntFix()).create()
        val values: Map<String, Any?> = gson.fromJson(json, mapType) ?: return 0

        val editor = prefs.edit()
        val themeEditor = themePrefs.edit()
        var restored = 0

        KEYS.forEach { key ->
            val value = values[key.name] ?: return@forEach
            val target = if (key.isTheme) themeEditor else editor
            if (target.put(key, value)) restored++
        }

        editor.apply()
        themeEditor.apply()
        return restored
    }

    /** False when the file's value does not have the key's type. */
    private fun SharedPreferences.Editor.put(key: Key, value: Any): Boolean {
        when (key.kind) {
            Kind.BOOLEAN -> putBoolean(key.name, value as? Boolean ?: return false)
            Kind.INT -> putInt(key.name, (value as? Number)?.toInt() ?: return false)
            Kind.LONG -> putLong(key.name, (value as? Number)?.toLong() ?: return false)
            Kind.STRING -> putString(key.name, value as? String ?: return false)
            //Gson writes a set as a JSON array, which reads back as a list
            Kind.STRING_SET -> putStringSet(key.name, (value as? List<*>)?.filterIsInstance<String>()?.toSet() ?: return false)
        }
        return true
    }
}

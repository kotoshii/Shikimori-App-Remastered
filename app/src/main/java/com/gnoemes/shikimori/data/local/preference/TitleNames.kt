package com.gnoemes.shikimori.data.local.preference

import com.gnoemes.shikimori.utils.nullIfEmpty

/**
 * Settings → «Русские названия»: on, the Russian name, or the original one when there is no Russian
 * name; off, the original. The one copy of this rule - every screen that shows a name calls it.
 */
fun SettingsSource.titleName(name: String, nameRu: String?): String =
        if (isRussianNaming) nameRu.nullIfEmpty() ?: name
        else name

/** The other name, shown under the main one on a title page. */
fun SettingsSource.secondTitleName(name: String, nameRu: String?): String =
        if (isRussianNaming) name
        else nameRu.nullIfEmpty() ?: name

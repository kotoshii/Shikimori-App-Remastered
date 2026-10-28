package com.gnoemes.shikimori.utils

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.Status

/**
 * A title's status in the site's words, which differ by type: anime «Анонс», «Онгоинг», «Вышло»;
 * manga and ranobe «Анонс», «Выходит», «Издано», «Приостановлено», «Прекращено» (BUGS #9, #16c).
 * «Нет данных» when it has none. The cards, the title pages, the forum's title block and the catalog's
 * «Статус» filter all take it from here.
 */
@StringRes
fun Status.labelRes(isAnime: Boolean): Int = when (this) {
    Status.ANONS -> R.string.status_anons
    Status.ONGOING -> if (isAnime) R.string.status_ongoing else R.string.status_publishing
    Status.RELEASED -> if (isAnime) R.string.status_released else R.string.status_published
    Status.PAUSED -> R.string.status_paused
    Status.DISCONTINUED -> R.string.status_discontinued
    Status.NONE -> R.string.error_no_data
}

/**
 * The colour a status is shown in, the site's own; null for one that is not shown in colour
 * (released, none) - those stay out of a card's line and of the title page's «Статус» row.
 */
@get:ColorRes
val Status.colorRes: Int?
    get() = when (this) {
        Status.ANONS -> R.color.status_anons
        Status.ONGOING -> R.color.status_ongoing
        Status.PAUSED -> R.color.status_paused
        Status.DISCONTINUED -> R.color.status_discontinued
        Status.RELEASED, Status.NONE -> null
    }

package com.gnoemes.shikimori.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.google.android.material.button.MaterialButton

/** The status icon, the same everywhere: the title page, the cards, the calendar, the status dialog. */
@get:DrawableRes
val RateStatus.iconRes: Int
    get() = when (this) {
        RateStatus.WATCHING -> R.drawable.ic_play_rate
        RateStatus.PLANNED -> R.drawable.ic_planned
        RateStatus.REWATCHING -> R.drawable.ic_replay
        RateStatus.COMPLETED -> R.drawable.ic_check
        RateStatus.ON_HOLD -> R.drawable.ic_pause_rate
        RateStatus.DROPPED -> R.drawable.ic_close
    }

/** «Смотрю», «Читаю», «Просмотрено»…: the status name for anime, or for manga and ranobe. */
@StringRes
fun RateStatus.nameRes(isAnime: Boolean): Int = when (this) {
    RateStatus.WATCHING -> if (isAnime) R.string.rate_watching else R.string.rate_reading
    RateStatus.PLANNED -> R.string.rate_planned
    RateStatus.REWATCHING -> if (isAnime) R.string.rate_rewatch else R.string.rate_rereading
    RateStatus.COMPLETED -> if (isAnime) R.string.rate_completed else R.string.rate_readed
    RateStatus.ON_HOLD -> R.string.rate_on_hold
    RateStatus.DROPPED -> R.string.rate_dropped
}

/**
 * The round status button on the title page and on the «Похожее» and «Хронология» cards: the status
 * icon in its colours, or "+" when the title is not in the list.
 */
fun MaterialButton.bindRateStatus(status: RateStatus?) {
    if (status == null) {
        setIconResource(R.drawable.ic_plus)
        iconTint = context.colorStateList(R.color.selector_circle_button_text_color)
        backgroundTintList = context.colorStateList(R.color.selector_gray_circle_background_color)
        rippleColor = context.colorStateList(R.color.selector_button_ripple_color)
        return
    }

    val (background, tint) = when (status) {
        RateStatus.PLANNED, RateStatus.WATCHING, RateStatus.REWATCHING -> Pair(R.color.rate_default_transparent, R.color.rate_default_dark)
        RateStatus.DROPPED -> Pair(R.color.rate_dropped_transparent, R.color.rate_dropped_dark)
        RateStatus.ON_HOLD -> Pair(R.color.rate_on_hold_transparent, R.color.rate_on_hold_dark)
        RateStatus.COMPLETED -> Pair(R.color.rate_watched_transparent, R.color.rate_watched_dark)
    }
    setIconResource(status.iconRes)
    backgroundTintList = context.colorStateList(background)
    setIconTintResource(tint)
    setRippleColorResource(context.attr(R.attr.colorDivider).resourceId)
}

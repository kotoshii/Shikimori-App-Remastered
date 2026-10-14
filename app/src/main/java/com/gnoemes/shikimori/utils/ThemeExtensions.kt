package com.gnoemes.shikimori.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Handler
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.ColorUtils
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.AscentTheme
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.app.domain.Theme
import com.gnoemes.shikimori.entity.app.domain.ThemeExtras
import org.joda.time.Duration
import org.joda.time.LocalTime

/** Each theme's style - the one map; the theme settings read it backwards to save a choice. */
@get:StyleRes
val Theme.style: Int
    get() = when (this) {
        Theme.DEFAULT -> R.style.ShikimoriAppTheme_Default
        Theme.DARK -> R.style.ShikimoriAppTheme_Dark
        Theme.AMOLED -> R.style.ShikimoriAppTheme_Amoled
    }

@get:StyleRes
val AscentTheme.style: Int
    get() = when (this) {
        AscentTheme.RED -> R.style.AscentStyle_Red
        AscentTheme.ORANGE -> R.style.AscentStyle_Orange
        AscentTheme.YELLOW -> R.style.AscentStyle_Yellow
        AscentTheme.GREEN -> R.style.AscentStyle_Green
        AscentTheme.CYAN -> R.style.AscentStyle_Cyan
        AscentTheme.BLUE -> R.style.AscentStyle_Blue
        AscentTheme.PURPLE -> R.style.AscentStyle_Purple
    }

private val Context.dayTheme: Theme
    get() {
        val themeIndex = getThemeSharedPreferences().getInt(ThemeExtras.THEME_KEY, Theme.DEFAULT.index)
        return Theme.values().find { it.index == themeIndex } ?: Theme.DEFAULT
    }

/** Null when no night theme is set. */
private val Context.nightTheme: Theme?
    get() {
        val themeIndex = getThemeSharedPreferences().getInt(ThemeExtras.NIGHT_THEME_KEY, Constants.NO_ID.toInt())
        return Theme.values().find { it.index == themeIndex }
    }

/** The theme on screen now: the night theme during its hours, the day theme otherwise. */
val Context.activeTheme: Theme
    get() = nightTheme?.takeIf { isNightTime } ?: dayTheme

/** Whether the theme on screen now is a dark one - at night too, unlike [getCurrentTheme]. */
val Context.isDarkTheme: Boolean
    get() = activeTheme != Theme.DEFAULT

/** The faint black or white (38%) of the «•» dividers and the small texts on the cards. */
@get:ColorInt
val Context.faintTextColor: Int
    get() = ColorUtils.setAlphaComponent(if (isDarkTheme) Color.WHITE else Color.BLACK, 97)

/** The day theme's style, the one picked in settings. */
val Context.getCurrentTheme: Int
    get() = dayTheme.style

val Context.getCurrentNightTheme: Int
    get() = nightTheme?.style ?: Constants.NO_ID.toInt()

val Context.getCurrentAscentTheme: Int
    get() {
        val themeIndex = getThemeSharedPreferences().getInt(ThemeExtras.ASCENT_KEY, AscentTheme.ORANGE.index)
        return (AscentTheme.values().find { it.index == themeIndex } ?: AscentTheme.ORANGE).style
    }

val Context.getNightThemeStartTime: LocalTime
    get() = LocalTime(getThemeSharedPreferences().getInt(ThemeExtras.NIGHT_THEME_START_KEY, LocalTime(20, 0, 0, 0).millisOfDay).toLong())

val Context.getNightThemeEndTime: LocalTime
    get() = LocalTime(getThemeSharedPreferences().getInt(ThemeExtras.NIGHT_THEME_END_KEY, LocalTime(8, 0, 0, 0).millisOfDay).toLong())

val Context.isNightTime: Boolean
    get() {
        val now = LocalTime.now()
        val startTime = getNightThemeStartTime
        val endTime = getNightThemeEndTime
        return if (startTime.isBefore(endTime)) startTime.isBeforeOrEqual(now) && now.isBeforeOrEqual(endTime)
        else startTime.isBeforeOrEqual(now) && now.isBeforeOrEqual(LocalTime(23, 59, 59, 999)) || LocalTime.MIDNIGHT.isBeforeOrEqual(now) && now.isBeforeOrEqual(endTime)
    }

fun Context.getThemeSharedPreferences(): SharedPreferences {
    return getSharedPreferences(packageName + "_theme" + "preferences", Context.MODE_PRIVATE)
}

fun Activity.setTheme(recreationRunnable: Runnable? = null) {
    setTheme(activeTheme.style)

    if (nightTheme != null) scheduleNightTheme(recreationRunnable)

    theme.applyStyle(getCurrentAscentTheme, true)
}

val themeHandler: Handler by lazy { Handler() }

fun Activity.scheduleNightTheme(runnable: Runnable?) {
    if (runnable == null) return
    val now = LocalTime.now()
    val startTime = getNightThemeStartTime
    val endTime = getNightThemeEndTime
    val lastDayTime = LocalTime(23, 59, 59, 999)
    val delayMills =
            if (isNightTime) {
                if (endTime.isBefore(now)) lastDayTime.millisOfDay - now.millisOfDay + endTime.millisOfDay
                else endTime.millisOfDay - now.millisOfDay
            } else {
                if (startTime.isBefore(now)) lastDayTime.millisOfDay - now.millisOfDay + startTime.millisOfDay
                else startTime.millisOfDay - now.millisOfDay
            }
    Log.i("NIGHT_THEME_KEY", "mills: $delayMills human: ${Duration.millis(delayMills.toLong())}")
    themeHandler.postDelayed(runnable, delayMills.toLong())
}

fun Context.wrapTheme(@StyleRes style: Int): ContextThemeWrapper {
    val baseWrap = ContextThemeWrapper(this, style)
    return ContextThemeWrapper(baseWrap, activeTheme.style)
}
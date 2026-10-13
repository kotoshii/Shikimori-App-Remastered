package com.gnoemes.shikimori.presentation.view.settings.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.timePicker
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.AscentTheme
import com.gnoemes.shikimori.entity.app.domain.Theme
import com.gnoemes.shikimori.entity.app.domain.ThemeExtras
import com.gnoemes.shikimori.presentation.view.common.widget.preferences.AscentPreference
import com.gnoemes.shikimori.presentation.view.common.widget.preferences.ThemePreference
import com.gnoemes.shikimori.presentation.view.settings.ToolbarCallback
import com.gnoemes.shikimori.utils.*
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalTime

class SettingsThemeFragment : BaseSettingsFragment(), Toolbar.OnMenuItemClickListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        (activity as? ToolbarCallback)?.showToolbarMenu()
        themePreference.apply {
            setTheme(context.getCurrentTheme)
            setNightTheme(context.getCurrentNightTheme)
            setNightModeStartTime(context.getNightThemeStartTime.millisOfDay)
            setNightModeEndTime(context.getNightThemeEndTime.millisOfDay)
            nightTimeStartClickListener = View.OnClickListener { showTimeDialog(getNightModeStartTime()) { setNightModeStartTime(it) } }
            nightTimeEndClickListener = View.OnClickListener { showTimeDialog(getNightModeEndTime()) { setNightModeEndTime(it) } }
        }

        ascentPreference.setAscentStyle(context!!.getCurrentAscentTheme)
    }

    private fun showTimeDialog(initValue: Int, action: (Int) -> Unit) {
        MaterialDialog(context!!).show {
            timePicker(currentTime = LocalTime(initValue.toLong()).toDateTimeToday().toGregorianCalendar()) { _, datetime ->
                action.invoke(DateTime(datetime).withZone(DateTimeZone.UTC).millisOfDay)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.item_accept) saveTheme()
        return true
    }

    private fun saveTheme() {
        prefs().putInt(ThemeExtras.THEME_KEY, getTheme(themePreference.getTheme()))
        prefs().putInt(ThemeExtras.ASCENT_KEY, getAscentTheme(ascentPreference.getAscentStyle()))
        prefs().putInt(ThemeExtras.NIGHT_THEME_KEY, getTheme(themePreference.getNightTheme()))
        prefs().putInt(ThemeExtras.NIGHT_THEME_START_KEY, themePreference.getNightModeStartTime())
        prefs().putInt(ThemeExtras.NIGHT_THEME_END_KEY, themePreference.getNightModeEndTime())
        activity?.onBackPressed()
    }

    private fun getAscentTheme(ascentStyle: Int): Int =
            (AscentTheme.values().find { it.style == ascentStyle } ?: AscentTheme.ORANGE).index

    //-1 is "no night theme"
    private fun getTheme(theme: Int): Int =
            Theme.values().find { it.style == theme }?.index ?: -1

    override fun onDestroy() {
        super.onDestroy()
        (activity as? ToolbarCallback)?.hideToolbarMenu()
    }

    private fun PreferenceFragmentCompat.prefs(): SharedPreferences = context!!.getThemeSharedPreferences()

    private val themePreference: ThemePreference
        get() = preference(ThemeExtras.THEME_KEY) as ThemePreference

    private val ascentPreference: AscentPreference
        get() = preference(ThemeExtras.ASCENT_KEY) as AscentPreference

    override val preferenceScreen: Int
        get() = R.xml.preferences_theme
}
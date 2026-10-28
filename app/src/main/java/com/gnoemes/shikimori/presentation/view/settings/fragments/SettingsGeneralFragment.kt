package com.gnoemes.shikimori.presentation.view.settings.fragments

import android.os.Bundle
import com.afollestad.materialdialogs.list.listItems
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.UserSource
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.entity.rates.domain.RateSwipeAction
import com.gnoemes.shikimori.utils.appFolder
import com.gnoemes.shikimori.utils.askStoragePermission
import com.gnoemes.shikimori.utils.chooseDownloadFolder
import com.gnoemes.shikimori.utils.preference
import com.gnoemes.shikimori.utils.prefs
import com.gnoemes.shikimori.utils.putString
import com.gnoemes.shikimori.utils.materialDialog
import javax.inject.Inject


class SettingsGeneralFragment : BaseSettingsFragment() {

    @Inject
    lateinit var userSource: UserSource

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        preference(R.string.settings_content_download_folder_key)?.apply {
            updateFolderSummary()
            setOnPreferenceClickListener { activity!!.askStoragePermission { context!!.chooseDownloadFolder(onChosen = { updateFolderSummary() }) };true }
        }

        preference(SettingsExtras.RATE_SWIPE_TO_LEFT_ACTION)?.apply {
            summary = getRateActionSummary(prefs().getString(key, RateSwipeAction.INCREMENT.name)!!)
            setOnPreferenceClickListener { showRateSwipeActionDialog(key); true }
        }

        preference(SettingsExtras.RATE_SWIPE_TO_RIGHT_ACTION)?.apply {
            summary = getRateActionSummary(prefs().getString(key, RateSwipeAction.CHANGE.name)!!)
            setOnPreferenceClickListener { showRateSwipeActionDialog(key); true }
        }

        //without an account the switch does nothing - SettingsSource.allowR18Content reports false
        //while signed out no matter what is stored - so offering it would be a lie
        preference(R.string.settings_content_allow_r18_key)?.isVisible = userSource.isSignedIn()

        preference(SettingsExtras.BACKUP_SETTINGS)?.apply {
            setOnPreferenceClickListener {
                activity!!.askStoragePermission {
                    BackupDialog().show(childFragmentManager, "BackupDialog")
                }
                true
            }
        }
    }

    private fun getRateActionSummary(action: String): String {
        return when (action) {
            RateSwipeAction.INCREMENT.name -> getString(R.string.rate_swipe_increment)
            RateSwipeAction.CHANGE.name -> getString(R.string.rate_swipe_change)
            RateSwipeAction.ON_HOLD.name -> getString(R.string.rate_swipe_hold)
            RateSwipeAction.DROP.name -> getString(R.string.rate_swipe_drop)
            RateSwipeAction.DISABLED.name -> getString(R.string.rate_swipe_disabled)
            else -> getString(R.string.rate_swipe_disabled)
        }
    }

    override val preferenceScreen: Int
        get() = R.xml.preferences_general

    private fun updateFolderSummary() {
        val folder = prefs().getString(SettingsExtras.DOWNLOAD_FOLDER, "")
        //the setting holds the folder the user picked; files go into the app's folder inside it,
        //so that is the path shown
        val summary =
                if (!folder.isNullOrEmpty()) appFolder(folder).absolutePath + "\n" +
                        context!!.getString(R.string.settings_content_download_folder_hint)
                else context!!.getString(R.string.settings_content_download_folder_summary)
        preference(SettingsExtras.DOWNLOAD_FOLDER)?.summary = summary
    }

    private fun showRateSwipeActionDialog(key: String) {
        context!!.materialDialog().show {
            listItems(R.array.rate_swipe_actions, waitForPositiveButton = false) { dialog, index, text ->
                preference(key)?.summary = text
                prefs().putString(key, RateSwipeAction.values()[index].name)
            }
        }
    }
}



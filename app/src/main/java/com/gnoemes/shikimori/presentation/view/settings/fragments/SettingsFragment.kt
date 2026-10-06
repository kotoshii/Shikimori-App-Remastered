package com.gnoemes.shikimori.presentation.view.settings.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.presentation.view.common.widget.preferences.AppGroupPreference
import com.gnoemes.shikimori.utils.preference
import com.gnoemes.shikimori.utils.toUri

class SettingsFragment : BaseSettingsFragment() {

    override val preferenceScreen: Int
        get() = R.xml.preferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        (preference("info_group") as? AppGroupPreference)?.apply {
            feedbackClickListener = View.OnClickListener { openWeb(Constants.GITHUB_ISSUES_URL) }
            forumClickListener = View.OnClickListener { openWeb(Constants.FOUR_PDA_THEME_URL) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView?.isVerticalScrollBarEnabled = false
    }

    private fun openWeb(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
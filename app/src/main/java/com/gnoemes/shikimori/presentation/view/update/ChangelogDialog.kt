package com.gnoemes.shikimori.presentation.view.update

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.services.impl.AppUpdateService
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetDialogFragment
import com.gnoemes.shikimori.utils.MarkdownRenderer
import com.gnoemes.shikimori.utils.addBackButton
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.getDefaultSharedPreferences
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.withArgs
import kotlinx.android.synthetic.main.dialog_base_bottom_sheet.*
import kotlinx.android.synthetic.main.dialog_changelog.*

/**
 * The release notes of the newest version, with a button that starts the download.
 *
 * The text is github markdown rendered by [MarkdownRenderer], which covers exactly the constructs
 * this project's release notes are written with - see BUILD_AND_RELEASE.md, which pins that set as
 * an authoring rule so notes and renderer cannot drift apart.
 *
 * Everything shown here comes from preferences rather than a fresh api call: the check that filled
 * them runs at app start, and the settings badge can be tapped hours later, possibly offline.
 */
class ChangelogDialog : BaseBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ChangelogDialog"

        private const val ARGUMENT_VERSION = "ARGUMENT_VERSION"
        private const val ARGUMENT_CHANGELOG = "ARGUMENT_CHANGELOG"
        private const val ARGUMENT_APK_URL = "ARGUMENT_APK_URL"

        fun newInstance(version: String, changelog: String?, apkUrl: String?) = ChangelogDialog().withArgs {
            putString(ARGUMENT_VERSION, version)
            putString(ARGUMENT_CHANGELOG, changelog)
            putString(ARGUMENT_APK_URL, apkUrl)
        }

        /**
         * The dialog for whatever release the last update check found, or null if it found none -
         * the caller then has nothing to show and should fall back to the releases page.
         */
        fun fromPreferences(context: Context): ChangelogDialog? {
            val prefs = context.getDefaultSharedPreferences()
            val version = prefs.getString(SettingsExtras.NEW_VERSION_TAG, null)

            return if (version.isNullOrBlank()) null
            else newInstance(version,
                    prefs.getString(SettingsExtras.NEW_VERSION_CHANGELOG, null),
                    prefs.getString(SettingsExtras.NEW_VERSION_APK_URL, null))
        }
    }

    private val version: String
        get() = arguments?.getString(ARGUMENT_VERSION).orEmpty()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        peekHeight = context.dimenAttr(android.R.attr.actionBarSize)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val changelog = arguments?.getString(ARGUMENT_CHANGELOG)
        //blank is treated as absent, so the button logic below is a plain null check
        val apkUrl = arguments?.getString(ARGUMENT_APK_URL)?.takeIf { it.isNotBlank() }

        with(toolbar) {
            title = getString(R.string.update_changelog_title, version)
            addBackButton(R.drawable.ic_close) { dismiss() }
        }

        //a release can be published without notes - v0.8.6.51 was - so an empty body says so
        //rather than showing an empty sheet
        changelogView.text =
                if (changelog.isNullOrBlank()) getString(R.string.update_changelog_empty)
                else MarkdownRenderer.render(changelog)
        //without this the links render as links and do nothing when tapped
        changelogView.movementMethod = LinkMovementMethod.getInstance()

        //nothing to download when the release has no apk attached; the notes are still worth reading
        downloadButton.visibility = if (apkUrl == null) View.GONE else View.VISIBLE
        downloadButton.onClick {
            val target = context
            if (target != null && apkUrl != null) AppUpdateService.download(target, version, apkUrl)
            //closing on download is deliberate: the progress is in the notification from here on
            dismiss()
        }

        closeButton.onClick { dismiss() }
    }

    override fun getDialogLayout(): Int = R.layout.dialog_changelog
}

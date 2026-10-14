package com.gnoemes.shikimori.presentation.view.update

import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.services.impl.AppUpdateService
import com.gnoemes.shikimori.entity.app.domain.AppUpdate
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetDialogFragment
import com.gnoemes.shikimori.utils.MarkdownRenderer
import com.gnoemes.shikimori.utils.addBackButton
import com.gnoemes.shikimori.utils.dimenAttr
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
 * Everything shown here is what the last update check stored, not a fresh api call: the check
 * runs at app start, and the settings badge can be tapped hours later, possibly offline.
 */
class ChangelogDialog : BaseBottomSheetDialogFragment() {

    companion object {
        const val TAG = "ChangelogDialog"

        private const val ARGUMENT_VERSION = "ARGUMENT_VERSION"
        private const val ARGUMENT_CHANGELOG = "ARGUMENT_CHANGELOG"
        private const val ARGUMENT_APK_URL = "ARGUMENT_APK_URL"

        /** The release the last update check found - see `AppUpdateSource.getAvailableUpdate`. */
        fun newInstance(update: AppUpdate) = ChangelogDialog().withArgs {
            putString(ARGUMENT_VERSION, update.version)
            putString(ARGUMENT_CHANGELOG, update.changelog)
            putString(ARGUMENT_APK_URL, update.apkUrl)
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
    }

    override fun getDialogLayout(): Int = R.layout.dialog_changelog
}

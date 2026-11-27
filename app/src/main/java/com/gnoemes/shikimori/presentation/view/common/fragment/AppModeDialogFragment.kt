package com.gnoemes.shikimori.presentation.view.common.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.AppMode
import com.gnoemes.shikimori.utils.isTvDevice
import com.gnoemes.shikimori.utils.materialDialog
import com.gnoemes.shikimori.utils.setAppMode

/**
 * Where the app is used, asked once at first launch and again from the «Оформление» settings
 * screen. The answer picks the whole interface behavior - the focus visuals and the text scale,
 * see AppModeExtensions.
 *
 * The device's own kind only chooses which option the dialog recommends: the stored answer, not
 * the device, is what the interface follows, so a user who prefers the TV's larger interface on a
 * phone gets it. Cancelling stores nothing, so the first launch asks again the next start.
 *
 * Changing the mode works like changing a theme: BaseThemedActivity watches the stored key and
 * rebuilds the stack.
 */
class AppModeDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "AppModeDialogFragment"

        fun newInstance() = AppModeDialogFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val recommendTv = context!!.isTvDevice

        return context!!.materialDialog().show {
            title(R.string.app_mode_title)
            message(if (recommendTv) R.string.app_mode_message_tv else R.string.app_mode_message)
            //material-dialogs auto-focuses its first (left) button, so the recommendation goes there:
            //a user pressing OK right away gets the suggested mode
            if (recommendTv) {
                negativeButton(R.string.app_mode_tv) { save(AppMode.TV) }
                positiveButton(R.string.app_mode_mobile) { save(AppMode.MOBILE) }
            } else {
                negativeButton(R.string.app_mode_mobile) { save(AppMode.MOBILE) }
                positiveButton(R.string.app_mode_tv) { save(AppMode.TV) }
            }
        }
    }

    private fun save(mode: AppMode) = context!!.setAppMode(mode)
}

package com.gnoemes.shikimori.presentation.view.series

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.afollestad.materialdialogs.list.listItems
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.series.domain.PlayerType
import com.gnoemes.shikimori.entity.series.presentation.EmbeddedPlayerNavigationData
import com.gnoemes.shikimori.presentation.view.base.fragment.MvpDialogFragment
import com.gnoemes.shikimori.utils.withArgs

/**
 * Which player opens a video. It carries the video and what the built-in player needs, as the download
 * dialogs do, so a tap still works after Android has killed the app and restored the dialog (BUGS #48,
 * T5-35). «Запомнить выбор» goes back with the pick, and the screen saves it (BUGS #33).
 */
class PlayerSelectDialog : MvpDialogFragment() {

    companion object {
        fun newInstance(playing: EmbeddedPlayerNavigationData) = PlayerSelectDialog().withArgs { putParcelable(PLAYING_KEY, playing) }
        private const val PLAYING_KEY = "PLAYING_KEY"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val playing = arguments?.getParcelable<EmbeddedPlayerNavigationData>(PLAYING_KEY)!!
        val callback = parentFragment as? Callback

        return MaterialDialog(context!!).show {
            //R.array.players lists them in PlayerType's order
            listItems(res = R.array.players) { dialog, index, _ ->
                callback?.onPlayerSelected(playing, PlayerType.values()[index], dialog.isCheckPromptChecked())
            }
            checkBoxPrompt(res = R.string.common_remember_choice, onToggle = null)
        }
    }

    interface Callback {
        fun onPlayerSelected(playing: EmbeddedPlayerNavigationData, playerType: PlayerType, remember: Boolean)
    }
}
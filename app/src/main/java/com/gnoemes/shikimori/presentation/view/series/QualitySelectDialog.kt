package com.gnoemes.shikimori.presentation.view.series

import android.app.Dialog
import android.os.Bundle
import com.afollestad.materialdialogs.list.listItems
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.series.domain.PlayerType
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.presentation.view.base.fragment.MvpDialogFragment
import com.gnoemes.shikimori.utils.withArgs
import com.gnoemes.shikimori.utils.materialDialog

/**
 * The quality to open in a player that takes one url («Внешний плеер», or «Веб-плеер» for a video with
 * no page of its own). Like [PlayerSelectDialog] it carries the video and the player, so a tap still
 * works after Android has killed the app and restored the dialog (BUGS #48, T5-35); and the parsed
 * video the tracks come from, which the outside player's headers are read from (BUGS #29).
 */
class QualitySelectDialog : MvpDialogFragment() {

    companion object {
        fun newInstance(video: TranslationVideo, playerType: PlayerType, parsed: Video) = QualitySelectDialog().withArgs {
            putParcelable(VIDEO_KEY, video)
            putSerializable(PLAYER_KEY, playerType)
            putParcelable(PARSED_KEY, parsed)
        }

        private const val VIDEO_KEY = "VIDEO_KEY"
        private const val PLAYER_KEY = "PLAYER_KEY"
        private const val PARSED_KEY = "PARSED_KEY"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val video = arguments?.getParcelable<TranslationVideo>(VIDEO_KEY)!!
        val playerType = arguments?.getSerializable(PLAYER_KEY) as PlayerType
        val parsed = arguments?.getParcelable<Video>(PARSED_KEY)!!
        val callback = parentFragment as? Callback

        //«720p», as the download list says it; this list showed «720» and «unknown» (BUGS #69 B5)
        val qualities = parsed.tracks.map { it.displayQuality ?: getString(R.string.download_quality_unknown) }

        return context!!.materialDialog().show {
            listItems(items = qualities) { _, index, _ ->
                callback?.onQualitySelected(video, playerType, parsed, parsed.tracks[index])
            }
        }
    }

    interface Callback {
        fun onQualitySelected(video: TranslationVideo, playerType: PlayerType, parsed: Video, track: Track)
    }
}

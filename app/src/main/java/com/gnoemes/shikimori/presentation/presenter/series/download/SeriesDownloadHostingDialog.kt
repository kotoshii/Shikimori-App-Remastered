package com.gnoemes.shikimori.presentation.presenter.series.download

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetDialogFragment
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.dp
import com.gnoemes.shikimori.utils.widgets.VerticalSpaceItemDecorator
import com.gnoemes.shikimori.utils.withArgs
import kotlinx.android.synthetic.main.dialog_base_bottom_sheet.*
import kotlinx.android.synthetic.main.dialog_series_download_hosting.*

/**
 * Which hosting to download from, asked before the quality list.
 *
 * One author usually publishes the same episode to half a dozen hostings, and the quality list used
 * to resolve every one of them up front - half a dozen page fetches, of which five were thrown away
 * the moment a quality was picked. Only the chosen hosting is resolved now.
 *
 * Laid out as the same cards as [SeriesDownloadDialog] rather than as a menu, because the two are
 * steps of one flow and are seen seconds apart.
 */
class SeriesDownloadHostingDialog : BaseBottomSheetDialogFragment() {

    companion object {
        fun newInstance(title: String, videos: List<TranslationVideo>, hasUnsupported: Boolean) = SeriesDownloadHostingDialog().withArgs {
            putString(TITLE_KEY, title)
            putParcelableArray(VIDEOS_KEY, videos.toTypedArray())
            putBoolean(UNSUPPORTED_KEY, hasUnsupported)
        }

        private const val TITLE_KEY = "TITLE_KEY"
        private const val VIDEOS_KEY = "VIDEOS_KEY"
        private const val UNSUPPORTED_KEY = "UNSUPPORTED_KEY"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        peekHeight = context.dimenAttr(android.R.attr.actionBarSize)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val videos = arguments?.getParcelableArray(VIDEOS_KEY)
                ?.map { it as TranslationVideo }
                ?: emptyList()

        with(toolbar) {
            title = arguments?.getString(TITLE_KEY)
        }

        //the group also holds hostings the app cannot resolve, and they are not in this list -
        //without a word about them the list simply looks short
        val hasUnsupported = arguments?.getBoolean(UNSUPPORTED_KEY) ?: false
        hostingUnsupportedNotice.visibility = if (hasUnsupported) View.VISIBLE else View.GONE

        val hostingAdapter = SeriesDownloadHostingAdapter(videos, (parentFragment as? Callback)) { dismiss() }

        with(recyclerView) {
            adapter = hostingAdapter
            layoutManager = LinearLayoutManager(context)
            val margin = context.dp(16)
            addItemDecoration(VerticalSpaceItemDecorator(context.dp(10), true, margin, margin))
        }
    }

    override fun getDialogLayout(): Int = R.layout.dialog_series_download_hosting

    interface Callback {
        fun onDownloadHostingSelected(video: TranslationVideo)
    }
}

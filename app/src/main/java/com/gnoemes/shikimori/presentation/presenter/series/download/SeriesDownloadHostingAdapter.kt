package com.gnoemes.shikimori.presentation.presenter.series.download

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import kotlinx.android.synthetic.main.item_download_hosting.view.*

class SeriesDownloadHostingAdapter(
        private val items: List<TranslationVideo>,
        private val callback: SeriesDownloadHostingDialog.Callback?,
        private val onAction: () -> Unit
) : RecyclerView.Adapter<SeriesDownloadHostingAdapter.ViewHolder>() {

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_download_hosting))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private lateinit var item: TranslationVideo

        init {
            itemView.hostingContainer.onClick { callback?.onDownloadHostingSelected(item); onAction.invoke() }
        }

        fun bind(item: TranslationVideo) {
            this.item = item
            itemView.hostingView.text = item.videoHosting.synonymType
        }
    }
}

package com.gnoemes.shikimori.presentation.view.series.episodes.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemSeriesEmptyPlaceholderBinding
import com.gnoemes.shikimori.entity.series.presentation.SeriesPlaceholderItem
import com.gnoemes.shikimori.utils.inflate
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

class SeriesPlaceholderAdapterDelegate : AbsListItemAdapterDelegate<SeriesPlaceholderItem, Any, SeriesPlaceholderAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is SeriesPlaceholderItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_series_empty_placeholder))

    override fun onBindViewHolder(item: SeriesPlaceholderItem, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemSeriesEmptyPlaceholderBinding.bind(view)

        fun bind(item: SeriesPlaceholderItem) {
            with(itemView) {
                binding.titleView.setText(item.title)
                binding.descriptionView.setText(item.description)
            }
        }

    }
}
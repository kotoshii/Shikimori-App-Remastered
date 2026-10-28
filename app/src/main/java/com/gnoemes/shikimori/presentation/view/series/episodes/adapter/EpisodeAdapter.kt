package com.gnoemes.shikimori.presentation.view.series.episodes.adapter

import com.gnoemes.shikimori.entity.series.presentation.EpisodePlaceholderItem
import com.gnoemes.shikimori.entity.series.presentation.EpisodeViewModel
import com.gnoemes.shikimori.presentation.view.common.adapter.BaseAdapter

class EpisodeAdapter(
        callback: (EpisodeViewModel) -> Unit,
        longPressListener: (EpisodeViewModel) -> Unit
) : BaseAdapter<Any>() {

    init {
        delegatesManager.addDelegate(EpisodeAdapterDelegate(callback, longPressListener))
        delegatesManager.addDelegate(EpisodePlaceholderAdapterDelegate())
        delegatesManager.addDelegate(SeriesPlaceholderAdapterDelegate())
    }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is EpisodeViewModel && newItem is EpisodeViewModel -> oldItem.index == newItem.index
        oldItem is EpisodePlaceholderItem && newItem is EpisodePlaceholderItem -> oldItem.index == newItem.index
        else -> false
    }

    /**
     * The loading placeholders are a state, not rows: to or from them the list is swapped at once. As
     * rows, all of them were removed and all the episodes added, and the list animated the removals
     * first - the sheet stood empty for a moment between the two (P7-T5 Q127).
     */
    override fun bindItems(newItems: List<Any>) {
        if (items.any { it is EpisodePlaceholderItem } || newItems.any { it is EpisodePlaceholderItem }) {
            //a copy first: given this adapter's own list, clearing would empty both
            val copy = newItems.toList()
            items.clear()
            items.addAll(copy)
            notifyDataSetChanged()
        } else super.bindItems(newItems)
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is EpisodeViewModel && newItem is EpisodeViewModel -> oldItem == newItem
        oldItem is EpisodePlaceholderItem && newItem is EpisodePlaceholderItem -> oldItem == newItem
        else -> false
    }
}

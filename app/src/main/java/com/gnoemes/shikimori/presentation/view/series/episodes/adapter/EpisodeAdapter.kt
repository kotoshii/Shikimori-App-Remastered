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

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is EpisodeViewModel && newItem is EpisodeViewModel -> oldItem == newItem
        oldItem is EpisodePlaceholderItem && newItem is EpisodePlaceholderItem -> oldItem == newItem
        else -> false
    }
}

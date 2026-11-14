package com.gnoemes.shikimori.presentation.view.search.filter.companies.adapter

import com.gnoemes.shikimori.entity.search.presentation.FilterEntryViewModel
import com.gnoemes.shikimori.presentation.view.common.adapter.BaseAdapter
import com.gnoemes.shikimori.presentation.view.search.filter.seasons.adapter.FilterEntryAdapterDelegate

/**
 * The chosen studios or publishers as tags with ✕, the typed seasons' look (FilterEntryAdapterDelegate): ✕
 * removes one, a long press excludes it or takes it back.
 */
class FilterTagAdapter(
        closeCallback: (FilterEntryViewModel) -> Unit,
        longPressCallback: (FilterEntryViewModel) -> Unit
) : BaseAdapter<Any>() {

    init {
        delegatesManager.addDelegate(FilterEntryAdapterDelegate(closeCallback, longPressCallback))
    }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean =
            oldItem is FilterEntryViewModel && newItem is FilterEntryViewModel && oldItem.value.removePrefix("!") == newItem.value.removePrefix("!")

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = oldItem == newItem
}

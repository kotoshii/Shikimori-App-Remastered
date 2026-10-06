package com.gnoemes.shikimori.presentation.view.search.filter.genres.adapter

import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterGenreSection
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.presentation.view.common.adapter.BaseAdapter

class FilterGenreAdapter(
        invertCallback: (FilterType, FilterViewModel) -> Unit,
        selectCallback: (FilterType, FilterViewModel) -> Unit
) : BaseAdapter<Any>() {

    init {
        delegatesManager.apply {
            addDelegate(FilterGenreSectionAdapterDelegate(invertCallback, selectCallback))
        }
    }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        //sections are told apart by their title, so swapping anime for manga rebinds them
        oldItem is FilterGenreSection && newItem is FilterGenreSection -> oldItem.titleRes == newItem.titleRes
        else -> false
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is FilterGenreSection && newItem is FilterGenreSection -> oldItem == newItem
        else -> false
    }
}
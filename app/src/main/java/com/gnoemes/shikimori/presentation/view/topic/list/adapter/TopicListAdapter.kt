package com.gnoemes.shikimori.presentation.view.topic.list.adapter

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.ProgressItem
import com.gnoemes.shikimori.entity.topic.presentation.TopicViewModel
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter
import com.gnoemes.shikimori.utils.images.ImageLoader

class TopicListAdapter(
        imageLoader: ImageLoader,
        navigationCallback: (Type, Long) -> Unit
) : BasePaginationAdapter() {

    init {
        delegatesManager.apply {
            addDelegate(TopicClubAdapterDelegate(imageLoader, navigationCallback))
            addDelegate(TopicLinkedAdapterDelegate(imageLoader, navigationCallback))
            setFallbackDelegate(TopicAdapterDelegate(imageLoader, navigationCallback))
        }
    }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is TopicViewModel && newItem is TopicViewModel -> oldItem.id == newItem.id
            else -> oldItem is ProgressItem && newItem is ProgressItem
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is TopicViewModel && newItem is TopicViewModel -> oldItem == newItem
            else -> false
        }
    }
}
package com.gnoemes.shikimori.presentation.view.friends.adapter

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.ProgressItem
import com.gnoemes.shikimori.entity.user.presentation.FriendViewModel
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter
import com.gnoemes.shikimori.utils.images.ImageLoader

class FriendsAdapter(
        imageLoader: ImageLoader,
        callback: (Type, Long) -> Unit
) : BasePaginationAdapter() {

    init {
        delegatesManager.addDelegate(FriendAdapterDelegate(imageLoader, callback))
    }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is FriendViewModel && newItem is FriendViewModel -> oldItem.id == newItem.id
        else -> oldItem is ProgressItem && newItem is ProgressItem
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is FriendViewModel && newItem is FriendViewModel -> oldItem == newItem
        else -> oldItem is ProgressItem && newItem is ProgressItem
    }
}
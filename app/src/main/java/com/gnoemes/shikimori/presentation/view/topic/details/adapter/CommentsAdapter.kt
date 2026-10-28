package com.gnoemes.shikimori.presentation.view.topic.details.adapter

import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.comment.presentation.CommentViewModel
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.PlaceholderItem
import com.gnoemes.shikimori.entity.common.presentation.ProgressItem
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter
import com.gnoemes.shikimori.utils.images.ImageLoader

class CommentsAdapter(
        imageLoader: ImageLoader,
        navigationCallback: (Type, Long) -> Unit
) : BasePaginationAdapter() {

    init {
        delegatesManager.addDelegate(CommentAdapterDelegate(imageLoader, navigationCallback))
        delegatesManager.addDelegate(CommentPlaceholderAdapterDelegate())
    }

    fun bindItems(newItems: List<Any>, previous: Boolean) {
        super.bindItems(if (!previous) newItems.takeLast(Constants.DEFAULT_LIMIT) else newItems)
    }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is CommentViewModel && newItem is CommentViewModel -> oldItem.id == newItem.id
            else -> oldItem is ProgressItem && newItem is ProgressItem
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is CommentViewModel && newItem is CommentViewModel -> oldItem == newItem
            else -> false
        }
    }

    //a new list each time: bindItems clears its own list before copying from the one it gets, so passing
    //that same list bound nothing and «Комментарии не найдены.» never showed (BUGS #38)
    fun showEmptyView() {
        super.bindItems(listOf(PlaceholderItem()))
    }

    //the placeholder only: the topic's own loading calls this too, and must not wipe loaded comments
    fun hideEmptyView() {
        if (items.any { it is PlaceholderItem }) super.bindItems(items.filterNot { it is PlaceholderItem })
    }

}
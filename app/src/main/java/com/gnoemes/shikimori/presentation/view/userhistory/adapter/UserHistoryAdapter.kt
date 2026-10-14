package com.gnoemes.shikimori.presentation.view.userhistory.adapter

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.ProgressItem
import com.gnoemes.shikimori.entity.user.presentation.UserHistoryHeaderViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserHistoryViewModel
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter

class UserHistoryAdapter(
        callback: (Type, Long) -> Unit
) : BasePaginationAdapter() {

    init {
        delegatesManager.apply {
            addDelegate(UserHistoryAdapterDelegate(callback))
            addDelegate(UserHistoryHeaderAdapterDelegate())
            addDelegate(UserHistoryWithoutTargetAdapterDelegate())
        }
    }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is UserHistoryHeaderViewModel && newItem is UserHistoryHeaderViewModel -> oldItem.date == newItem.date
        oldItem is UserHistoryViewModel && newItem is UserHistoryViewModel -> oldItem.id == newItem.id
        else -> oldItem is ProgressItem && newItem is ProgressItem
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is UserHistoryHeaderViewModel && newItem is UserHistoryHeaderViewModel -> oldItem == newItem
        oldItem is UserHistoryViewModel && newItem is UserHistoryViewModel -> oldItem == newItem
        else -> oldItem is ProgressItem && newItem is ProgressItem
    }
}
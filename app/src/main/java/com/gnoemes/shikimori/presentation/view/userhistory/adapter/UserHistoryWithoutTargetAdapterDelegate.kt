package com.gnoemes.shikimori.presentation.view.userhistory.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemUserHistoryWithoutTargetBinding
import com.gnoemes.shikimori.entity.user.presentation.UserHistoryViewModel
import com.gnoemes.shikimori.utils.inflate
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

class UserHistoryWithoutTargetAdapterDelegate : AbsListItemAdapterDelegate<UserHistoryViewModel, Any, UserHistoryWithoutTargetAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is UserHistoryViewModel && item.target == null

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_user_history_without_target))

    override fun onBindViewHolder(item: UserHistoryViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemUserHistoryWithoutTargetBinding.bind(view)

        fun bind(item: UserHistoryViewModel) {
            with(itemView) {
                binding.actionView.text = item.action
                binding.dateView.text = item.actionDateString
            }
        }

    }
}
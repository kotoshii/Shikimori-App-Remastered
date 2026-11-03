package com.gnoemes.shikimori.presentation.view.common.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemDetailsInfoBinding
import com.gnoemes.shikimori.entity.common.presentation.InfoItem
import com.gnoemes.shikimori.utils.inflate
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

class InfoAdapterDelegate : AbsListItemAdapterDelegate<InfoItem, Any, InfoAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is InfoItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_details_info))

    override fun onBindViewHolder(item: InfoItem, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemDetailsInfoBinding.bind(view)

        fun bind(item: InfoItem) {
            with(itemView) {
                binding.infoView.text = item.description
                binding.categoryView.text = item.category
            }
        }
    }
}
package com.gnoemes.shikimori.presentation.view.common.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemPageErrorBinding
import com.gnoemes.shikimori.entity.common.presentation.PageErrorItem
import com.gnoemes.shikimori.utils.inflate
import com.hannesdorfmann.adapterdelegates4.AdapterDelegate

class PageErrorAdapterDelegate(
        private val retry: () -> Unit
) : AdapterDelegate<MutableList<Any>>() {
    override fun isForViewType(items: MutableList<Any>, position: Int) =
            items[position] is PageErrorItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_page_error)).apply {
                binding.networkErrorView.apply {
                    hideImage()
                    showButton()
                    callback = { retry() }
                }
            }

    override fun onBindViewHolder(
            items: MutableList<Any>,
            position: Int,
            viewHolder: RecyclerView.ViewHolder,
            payloads: MutableList<Any>
    ) = Unit

    private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemPageErrorBinding.bind(view)
    }
}

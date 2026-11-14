package com.gnoemes.shikimori.presentation.view.search.filter.seasons.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemChipEntryBinding
import com.gnoemes.shikimori.entity.search.presentation.FilterEntryViewModel
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onLongPress
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

/** A tag: ✕ removes it; a long press excludes it or takes it back, as on the filter chips (P10-T4). */
class FilterEntryAdapterDelegate(
        private val closeCallback: (FilterEntryViewModel) -> Unit,
        private val longPressCallback: (FilterEntryViewModel) -> Unit
) : AbsListItemAdapterDelegate<FilterEntryViewModel, Any, FilterEntryAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is FilterEntryViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_chip_entry))

    override fun onBindViewHolder(item: FilterEntryViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemChipEntryBinding.bind(view)

        private lateinit var item: FilterEntryViewModel

        init {
            binding.chip.setOnCloseIconClickListener { closeCallback.invoke(item) }
            binding.chip.onLongPress { longPressCallback.invoke(item) }
        }

        fun bind(item: FilterEntryViewModel) {
            this.item = item
            binding.chip.text = item.text
            //an excluded studio or publisher: the filter chips' − before the name
            binding.chip.isChipIconVisible = item.excluded
            if (item.excluded) binding.chip.setChipIconResource(R.drawable.ic_minus)
        }
    }
}
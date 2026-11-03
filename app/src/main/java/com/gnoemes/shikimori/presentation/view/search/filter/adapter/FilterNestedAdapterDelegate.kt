package com.gnoemes.shikimori.presentation.view.search.filter.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemCategoryWithButtonBinding
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterAction
import com.gnoemes.shikimori.entity.search.presentation.FilterNestedViewModel
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.visibleIf
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

class FilterNestedAdapterDelegate(
        private val actionCallback: (FilterType, FilterAction) -> Unit
) : AbsListItemAdapterDelegate<FilterNestedViewModel, Any, FilterNestedAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is FilterNestedViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_category_with_button))

    override fun onBindViewHolder(item: FilterNestedViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemCategoryWithButtonBinding.bind(view)

        private lateinit var item: FilterNestedViewModel

        init {
            binding.countBtn.onClick { actionCallback.invoke(item.type, FilterAction.ShowNested) }
            binding.container.onClick { actionCallback.invoke(item.type, FilterAction.ShowNested) }
            binding.clearBtn.onClick { actionCallback.invoke(item.type, FilterAction.Clear) }
        }

        fun bind(item: FilterNestedViewModel) {
            this.item = item
            with(itemView) {
                val countText = if (item.appliedCount > 0) "${item.appliedCount}" else "+"
                binding.countBtn.text = countText
                binding.categoryName.text = item.categoryLocalised

                binding.countBtn.isSelected = item.appliedCount > 0
                binding.clearBtn.visibleIf { item.appliedCount > 0 }
            }
        }

    }
}
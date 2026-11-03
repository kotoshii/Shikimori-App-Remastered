package com.gnoemes.shikimori.presentation.view.search.filter.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemCategoryWithChipGroupAndButtonsBinding
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterAction
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.entity.search.presentation.FilterWithButtonsViewModel
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.visibleIf
import com.google.android.flexbox.FlexboxLayoutManager
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

class FilterWithButtonsAdapterDelegate(
        private val invertCallback: (FilterType, FilterViewModel) -> Unit,
        private val selectCallback: (FilterType, FilterViewModel) -> Unit,
        private val actionCallback: (FilterType, FilterAction) -> Unit
) : AbsListItemAdapterDelegate<FilterWithButtonsViewModel, Any, FilterWithButtonsAdapterDelegate.ViewHolder>() {

    private val sharedPool = RecyclerView.RecycledViewPool()

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is FilterWithButtonsViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_category_with_chip_group_and_buttons))

    override fun onBindViewHolder(item: FilterWithButtonsViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemCategoryWithChipGroupAndButtonsBinding.bind(view)

        private lateinit var item: FilterWithButtonsViewModel


        init {
            with(itemView) {
                binding.chipList.setRecycledViewPool(sharedPool)
                binding.chipList.itemAnimator = null
                binding.clearBtn.onClick { actionCallback.invoke(item.type, FilterAction.Clear) }
                binding.invertBtn.onClick { actionCallback.invoke(item.type, FilterAction.Invert) }
                binding.checkAllBtn.onClick { actionCallback.invoke(item.type, FilterAction.SelectAll) }
            }
        }

        fun bind(item: FilterWithButtonsViewModel) {
            this.item = item
            with(itemView) {
                binding.categoryNameView.text = item.categoryLocalised

                val chipAdapter = FilterChipAdapter(item.type, invertCallback, selectCallback).apply { if (!hasObservers()) setHasStableIds(true) }

                with(binding.chipList) {
                    adapter = chipAdapter
                    layoutManager = FlexboxLayoutManager(context)
                }

                chipAdapter.bind(item.filters)

                val filtersApplied = item.filters.find { it.state != FilterViewModel.STATE.DEFAULT } != null

                binding.clearBtn.visibleIf { filtersApplied }
                binding.invertBtn.visibleIf { item.hasInvert && filtersApplied }
                binding.checkAllBtn.visibleIf { item.hasSelectAll }
            }
        }

    }
}
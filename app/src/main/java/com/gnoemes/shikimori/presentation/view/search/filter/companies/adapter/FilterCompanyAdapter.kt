package com.gnoemes.shikimori.presentation.view.search.filter.companies.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemFilterCompanyBinding
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.utils.clearAndAddAll
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick

/**
 * The studios or publishers, one row each: a tap picks or drops one, a long press excludes one or takes
 * it back, as on a chip. A plain adapter, not a diffed one: the list is redrawn on every letter typed,
 * and a diff of some 1900 rows that change at once would cost more than drawing the rows on screen.
 */
class FilterCompanyAdapter(
        private val type: FilterType,
        private val selectCallback: (FilterType, FilterViewModel) -> Unit,
        private val invertCallback: (FilterType, FilterViewModel) -> Unit
) : RecyclerView.Adapter<FilterCompanyAdapter.ViewHolder>() {

    private val items = mutableListOf<FilterViewModel>()

    init {
        setHasStableIds(true)
    }

    fun bind(newItems: List<FilterViewModel>) {
        items.clearAndAddAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    //the id, as an excluded one is "!id"
    override fun getItemId(position: Int): Long =
            items[position].value.removePrefix("!").toLongOrNull() ?: RecyclerView.NO_ID

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_filter_company))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = ItemFilterCompanyBinding.bind(view)

        private lateinit var item: FilterViewModel

        init {
            binding.root.onClick { selectCallback.invoke(type, item) }
            binding.root.setOnLongClickListener { invertCallback.invoke(type, item); true }
        }

        fun bind(item: FilterViewModel) {
            this.item = item
            binding.name.text = item.text
            //the chips' marks: ✓ picked, − excluded
            when (item.state) {
                FilterViewModel.STATE.DEFAULT -> binding.stateIcon.visibility = View.INVISIBLE
                FilterViewModel.STATE.SELECTED -> binding.stateIcon.apply { setImageResource(R.drawable.ic_check); visibility = View.VISIBLE }
                FilterViewModel.STATE.INVERTED -> binding.stateIcon.apply { setImageResource(R.drawable.ic_minus); visibility = View.VISIBLE }
            }
        }
    }
}

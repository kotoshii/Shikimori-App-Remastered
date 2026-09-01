package com.gnoemes.shikimori.presentation.view.settings.fragments

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.utils.clearAndAddAll
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.visibleIf
import kotlinx.android.synthetic.main.item_hosting_filter.view.*

/** One row per hosting: ticked when it is hidden, with a delete button to drop it from the list. */
class HostingFilterAdapter(
        private val toggleCallback: (String, Boolean) -> Unit,
        private val deleteCallback: (String) -> Unit
) : RecyclerView.Adapter<HostingFilterAdapter.ViewHolder>() {

    data class Item(val domain: String, val hidden: Boolean, val canDelete: Boolean)

    private val items = mutableListOf<Item>()

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_hosting_filter))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    fun bindItems(newItems: List<Item>) {
        items.clearAndAddAll(newItems)
        notifyDataSetChanged()
    }

    fun remove(domain: String) {
        val index = items.indexOfFirst { it.domain == domain }
        if (index < 0) return

        items.removeAt(index)
        notifyItemRemoved(index)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private lateinit var item: Item

        init {
            //the whole row toggles - a bigger target than the box, which is why the box itself is
            //not clickable in the layout
            itemView.onClick { toggle() }
            itemView.deleteView.onClick { deleteCallback.invoke(item.domain) }
        }

        fun bind(item: Item) {
            this.item = item
            itemView.hostingName.text = item.domain
            itemView.hostingCheckBox.isChecked = item.hidden
            itemView.deleteView.visibleIf { item.canDelete }
        }

        private fun toggle() {
            val position = adapterPosition
            if (position == RecyclerView.NO_POSITION) return

            val hidden = !itemView.hostingCheckBox.isChecked

            itemView.hostingCheckBox.isChecked = hidden
            //kept in sync, so rebinding this row after a delete does not show a stale tick
            item = item.copy(hidden = hidden)
            items[position] = item

            toggleCallback.invoke(item.domain, hidden)
        }
    }
}

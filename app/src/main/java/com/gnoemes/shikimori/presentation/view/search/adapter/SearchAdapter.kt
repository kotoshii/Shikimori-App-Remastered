package com.gnoemes.shikimori.presentation.view.search.adapter

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.ProgressItem
import com.gnoemes.shikimori.entity.search.presentation.SearchItem
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter
import com.gnoemes.shikimori.utils.images.ImageLoader

class SearchAdapter(
        imageLoader: ImageLoader,
        callback: (Type, Long) -> Unit
) : BasePaginationAdapter() {

    private val cards = SearchItemAdapterDelegate(imageLoader, callback)

    init {
        delegatesManager.apply {
            addDelegate(cards)
        }
    }

    /** The sizes for the grid's card width (P10-T3): the cards on screen are drawn again when it changes. */
    fun setLook(look: CatalogCardLook) {
        if (look == cards.look) return
        cards.look = look
        notifyItemRangeChanged(0, itemCount)
    }

    //a card's id is its title's (or character's, person's). The base's id, the whole item's hashCode, changed
    //each time the cards were made: the line under the name is coloured text, whose hashCode takes its colour
    //spans' own, and those have none but the object's. The cards are made again for every page, so every
    //card on screen got a new id, was drawn again and its poster reloaded - the cards blinked (P10-T3 Q12)
    override fun getItemId(position: Int): Long = when (val item = items[position]) {
        is SearchItem -> item.id * Type.values().size + item.type.ordinal
        else -> super.getItemId(position)
    }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is SearchItem && newItem is SearchItem -> oldItem.id == newItem.id && oldItem.type == newItem.type
            else -> oldItem is ProgressItem && newItem is ProgressItem
        }
    }

    //the line under the name is styled text, compared by its words: the list is converted again for each
    //page and each changed setting, which makes new text objects for the same words
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is SearchItem && newItem is SearchItem ->
                oldItem.copy(description = null) == newItem.copy(description = null)
                        && oldItem.description?.toString() == newItem.description?.toString()
            else -> false
        }
    }
}

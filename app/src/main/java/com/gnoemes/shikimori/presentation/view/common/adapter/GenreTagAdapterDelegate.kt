package com.gnoemes.shikimori.presentation.view.common.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import com.gnoemes.shikimori.entity.common.presentation.DetailsAction
import com.gnoemes.shikimori.entity.common.presentation.DetailsTagItem
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.google.android.material.chip.Chip
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

/**
 * Every genre draws the same, whatever its [GenreV2.Kind]. The kinds are told apart by **order** -
 * demographic, then genres, then themes, see `AnimeDetailsViewModelConverterImpl` - and not by
 * colour: all of these chips do the same thing when tapped, and in this app a faint accent fill
 * already means *selected* (`FilterChipAdapter`), which a details screen has no notion of.
 */
class GenreTagAdapterDelegate(
        private val callback: (DetailsAction) -> Unit
) : AbsListItemAdapterDelegate<DetailsTagItem, Any, GenreTagAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is DetailsTagItem && item.type == DetailsTagItem.TagType.GENRE

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_tag_genre))

    override fun onBindViewHolder(item: DetailsTagItem, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private lateinit var item: DetailsTagItem

        init {
            itemView.onClick { callback.invoke(DetailsAction.GenreClicked(item.raw as GenreV2)) }
        }

        fun bind(item: DetailsTagItem) {
            this.item = item
            (itemView as Chip).text = item.name
        }

    }
}

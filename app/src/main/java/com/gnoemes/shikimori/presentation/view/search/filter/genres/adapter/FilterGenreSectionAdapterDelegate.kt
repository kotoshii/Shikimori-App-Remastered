package com.gnoemes.shikimori.presentation.view.search.filter.genres.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterGenreSection
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.utils.dimen
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.google.android.material.chip.Chip
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate
import kotlinx.android.synthetic.main.item_filter_genre_flex_section.view.*

/**
 * A titled block of genre chips - demographic, genres or themes.
 *
 * ⚠️ The chips are inflated straight into a `FlexboxLayout` rather than bound by a nested
 * RecyclerView, which is what `item_filter_genre_section` and its delegate do. That arrangement
 * measures `wrap_content` against only some of its children inside the sheet's own scrolling list,
 * and the themes section is large enough to hit it: everything after "Супер сила" was never laid
 * out, CGDCT with it, and the section drew a phantom gap where a chip belonged. Nothing here needs
 * recycling - a section is a fixed list of at most ~55 chips, built once.
 */
class FilterGenreSectionAdapterDelegate(
        private val invertCallback: (FilterType, FilterViewModel) -> Unit,
        private val selectCallback: (FilterType, FilterViewModel) -> Unit
) : AbsListItemAdapterDelegate<FilterGenreSection, Any, FilterGenreSectionAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is FilterGenreSection

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_filter_genre_flex_section))

    override fun onBindViewHolder(item: FilterGenreSection, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val smallPadding by lazy { itemView.context.dimen(R.dimen.margin_small) }
        private val normalPadding by lazy { itemView.context.dimen(R.dimen.margin_normal) }

        fun bind(item: FilterGenreSection) {
            itemView.categoryNameView.setText(item.titleRes)

            with(itemView.chipContainer) {
                //rebound wholesale: a section only changes when a filter is applied or the content
                //type is switched, and both replace every chip in it
                removeAllViews()

                item.filters.forEach { filter -> addView(createChip(this, filter)) }
            }
        }

        /**
         * The same `item_chip_filter` the filter sheet uses everywhere, with the state handling
         * `FilterChipAdapter` applies - kept in step with it deliberately, so a chip looks and
         * behaves the same wherever it appears.
         */
        private fun createChip(parent: ViewGroup, filter: FilterViewModel): Chip {
            val chip = parent.inflate(R.layout.item_chip_filter) as Chip

            chip.text = filter.text
            chip.isChipIconVisible = filter.state == FilterViewModel.STATE.INVERTED

            when (filter.state) {
                FilterViewModel.STATE.DEFAULT -> chip.apply {
                    isSelected = false; isChecked = false; textStartPadding = normalPadding
                }
                FilterViewModel.STATE.INVERTED -> chip.apply {
                    isSelected = false; isChecked = true; textStartPadding = smallPadding
                }
                FilterViewModel.STATE.SELECTED -> chip.apply {
                    isSelected = true; isChecked = false; textStartPadding = normalPadding
                }
            }

            chip.onClick { selectCallback.invoke(FilterType.GENRE, filter) }
            chip.setOnLongClickListener { invertCallback.invoke(FilterType.GENRE, filter); true }

            return chip
        }
    }
}

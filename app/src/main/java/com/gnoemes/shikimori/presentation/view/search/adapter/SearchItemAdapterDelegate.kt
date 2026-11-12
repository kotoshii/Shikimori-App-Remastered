package com.gnoemes.shikimori.presentation.view.search.adapter

import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemSearchBinding
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.presentation.SearchItem
import com.gnoemes.shikimori.utils.badgeColorRes
import com.gnoemes.shikimori.utils.iconRes
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.visibleIf
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate
import kotlin.math.roundToInt

class SearchItemAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val callback: (Type, Long) -> Unit
) : AbsListItemAdapterDelegate<SearchItem, Any, SearchItemAdapterDelegate.ViewHolder>() {

    /** The sizes for the grid's card width; the adapter draws the cards again when it changes. */
    var look: CatalogCardLook = CatalogCardLook.LARGE

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is SearchItem

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_search))

    override fun onBindViewHolder(item: SearchItem, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemSearchBinding.bind(view)

        private lateinit var item: SearchItem

        //the look this card is drawn in: set again only when the grid's card width changed
        private var drawnLook: CatalogCardLook? = null

        private val star = ContextCompat.getDrawable(view.context, R.drawable.ic_mini_star)!!.mutate()
                .apply { setTint(Color.WHITE) }

        init {
            //the whole card, the name under the poster too
            binding.container.onClick { callback.invoke(item.type, item.id) }
        }

        fun bind(item: SearchItem) {
            this.item = item
            applyLook(look)

            imageLoader.setImageListItem(binding.imageView, item.image.original)

            binding.typeView.text = item.typeText
            binding.typeView.visibleIf { item.typeText != null }

            item.status?.let {
                binding.rateView.setIconResource(it.iconRes)
                binding.rateView.setIconTintResource(it.badgeColorRes)
            }
            binding.rateView.visibleIf { item.status != null }

            binding.scoreView.text = item.score
            binding.scoreView.visibleIf { item.score != null }

            binding.nameView.text = item.name
            binding.nameView.visibleIf { item.name != null }

            //an empty line keeps its place, so the cards of a row stay the same height
            binding.descriptionView.text = item.description
            binding.descriptionView.visibility = when {
                item.description == null -> View.GONE
                item.description.isEmpty() -> View.INVISIBLE
                else -> View.VISIBLE
            }
        }

        private fun applyLook(look: CatalogCardLook) {
            if (look == drawnLook) return
            drawnLook = look

            val metrics = itemView.resources.displayMetrics
            fun dp(value: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), metrics).roundToInt()
            //the chips' own sizes follow their text, so a bigger system font keeps the text inside
            fun ofText(sp: Float, share: Float): Int =
                    (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics) * share).roundToInt()

            with(binding.typeView) {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, look.typeSp)
                setPadding(ofText(look.typeSp, 0.45f), ofText(look.typeSp, 0.3f), ofText(look.typeSp, 0.45f), ofText(look.typeSp, 0.3f))
            }

            with(binding.scoreView) {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, look.scoreSp)
                val pad = ofText(look.scoreSp, 0.583f)
                setPadding(pad, 0, pad, 0)
                updateLayoutParams<ConstraintLayout.LayoutParams> {
                    height = ofText(look.scoreSp, 1.667f)
                    marginEnd = pad
                    rightMargin = pad
                    bottomMargin = pad
                }
                val starSize = ofText(look.scoreSp, 0.95f)
                star.setBounds(0, 0, starSize, starSize)
                setCompoundDrawablesRelative(star, null, null, null)
            }

            with(binding.rateView) {
                updateLayoutParams<ConstraintLayout.LayoutParams> {
                    width = dp(look.badgeDp)
                    height = dp(look.badgeDp)
                    topMargin = dp(look.badgeInsetDp)
                    marginEnd = dp(look.badgeInsetDp)
                    rightMargin = dp(look.badgeInsetDp)
                }
                iconSize = dp(look.badgeIconDp)
            }

            binding.nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, look.nameSp)
            binding.descriptionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, look.lineSp)
        }
    }
}

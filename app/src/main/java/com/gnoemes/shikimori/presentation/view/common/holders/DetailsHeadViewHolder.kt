package com.gnoemes.shikimori.presentation.view.common.holders

import android.view.View
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.LayoutDetailsHeadBinding
import com.gnoemes.shikimori.entity.common.presentation.DetailsAction
import com.gnoemes.shikimori.entity.common.presentation.DetailsHeadItem
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader

class DetailsHeadViewHolder(
        private val view: View,
        private val imageLoader: ImageLoader,
        private val callback: (DetailsAction) -> Unit
) {

    private val binding = LayoutDetailsHeadBinding.bind(view)

    private val placeholder: DetailsPlaceholderViewHolder by lazy { DetailsPlaceholderViewHolder(binding.headContent.root, binding.headPlaceholder.root) }

    init {
        with(binding.headContent) {
            editRateBtn.onClick { callback.invoke(DetailsAction.EditRate) }
            statusRateBtn.onClick { callback.invoke(DetailsAction.RateStatusDialog) }

            ratingView.setEmptyDrawable(root.context.drawable(R.drawable.ic_small_star_empty)?.apply { tint(root.context!!.colorAttr(R.attr.colorOnPrimarySecondary)) })
            ratingView.setFilledDrawable(root.context.drawable(R.drawable.ic_small_star_filled)?.apply { tint(root.context!!.colorAttr(R.attr.colorSecondary)) })
        }
    }

    fun bind(item: DetailsHeadItem) {
        placeholder.showContent()

        with(binding.headContent) {
            if (!imageView.hasImage()) {
                imageLoader.setImageWithPlaceHolder(imageView, item.image.original)
            }

            nameView.text = item.name

            ratingView.rating = (item.score / 2).toFloat()
            ratingValueView.text = item.score.toString()


            editRateBtn.visibleIf { !item.isGuest }
            statusRateBtn.visibleIf { !item.isGuest }

            statusRateBtn.bindRateStatus(item.rateStatus)
        }
    }
}
package com.gnoemes.shikimori.presentation.view.common.holders

import android.view.View
import com.gnoemes.shikimori.databinding.LayoutDetailsDescriptionBinding
import com.gnoemes.shikimori.entity.common.presentation.DetailsDescriptionItem
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.visible

class DetailsDescriptionViewHolder(
        private val view: View,
        imageLoader: ImageLoader,
        listener: RichTextListener
) {

    private val binding = LayoutDetailsDescriptionBinding.bind(view)

    private val placeholder by lazy { DetailsPlaceholderViewHolder(binding.descriptionContent.root, binding.descriptionPlaceholder.root) }

    init {
        binding.descriptionContent.descriptionTextView.setup(imageLoader, listener)
    }

    fun bind(item: DetailsDescriptionItem) {

        if (item.description.isEmpty) {
            view.gone()
            return
        }

        //shown again too: a section that hides itself when empty must show itself when full (CODE_CONVENTIONS §8)
        view.visible()
        placeholder.showContent()

        binding.descriptionContent.descriptionTextView.setContent(item.description)
    }


}

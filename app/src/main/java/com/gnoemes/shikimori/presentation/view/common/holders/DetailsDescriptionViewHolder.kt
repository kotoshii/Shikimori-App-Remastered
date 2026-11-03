package com.gnoemes.shikimori.presentation.view.common.holders

import android.view.View
import com.gnoemes.shikimori.databinding.LayoutDetailsDescriptionBinding
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.DetailsDescriptionItem
import com.gnoemes.shikimori.utils.gone

class DetailsDescriptionViewHolder(
        private val view: View,
        navigationCallback: (Type, Long) -> Unit
) {

    private val binding = LayoutDetailsDescriptionBinding.bind(view)

    private val placeholder by lazy { DetailsPlaceholderViewHolder(binding.descriptionContent.root, binding.descriptionPlaceholder.root) }

    init {
        binding.descriptionContent.descriptionTextView.linkCallback = navigationCallback
    }

    fun bind(item: DetailsDescriptionItem) {

        if (item.description.isNullOrEmpty()) {
            view.gone()
            return
        }

        placeholder.showContent()

        binding.descriptionContent.descriptionTextView.setContent(item.description)
    }


}
package com.gnoemes.shikimori.presentation.view.topic.holders

import android.view.View
import com.gnoemes.shikimori.databinding.LayoutTopicBinding
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.entity.topic.presentation.TopicContentViewModel
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.visibleIf

class TopicContentViewHolder(
        private val view: View,
        imageLoader: ImageLoader,
        listener: RichTextListener,
        private val expandable: Boolean = false
) {

    private val binding = LayoutTopicBinding.bind(view)
    private lateinit var item: TopicContentViewModel

    init {
        binding.contentView.expandable = expandable
        binding.contentView.setup(imageLoader, listener)
    }

    fun bind(item: TopicContentViewModel) {
        this.item = item
        with(view) {
            binding.titleView.text = item.title
            binding.contentView.visibleIf { !item.content.isEmpty }
            binding.contentView.setContent(item.content)
        }
    }
}

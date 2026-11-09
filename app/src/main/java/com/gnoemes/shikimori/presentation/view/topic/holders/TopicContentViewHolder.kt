package com.gnoemes.shikimori.presentation.view.topic.holders

import android.view.View
import com.gnoemes.shikimori.databinding.LayoutTopicBinding
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextState
import com.gnoemes.shikimori.entity.topic.presentation.TopicContentViewModel
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.visibleIf

/**
 * A forum row's title and post (layout_topic). The post is cut at 80dp with the chevron, as a comment, and
 * builds only what fills the cut; a tap on its plain words opens the topic, as a tap on the rest of the row
 * does (P8-T4 plan, §1; both set in the layout).
 */
class TopicContentViewHolder(
        view: View,
        imageLoader: ImageLoader,
        listener: RichTextListener
) {

    private val binding = LayoutTopicBinding.bind(view)

    init {
        binding.contentView.setup(imageLoader, listener)
    }

    /** [state]: what the user opened in this row's post, kept by the list while it lives. */
    fun bind(item: TopicContentViewModel, state: RichTextState) {
        binding.titleView.text = item.title
        binding.contentView.visibleIf { !item.content.isEmpty }
        binding.contentView.setContentState(state)
    }
}

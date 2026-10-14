package com.gnoemes.shikimori.presentation.view.topic.holders

import android.view.View
import android.widget.TextView
import com.gnoemes.shikimori.entity.topic.presentation.TopicLinkedViewModel
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.visibleIf
import kotlinx.android.synthetic.main.layout_topic_linked.view.*

/**
 * The title block, the same in the forum list's title-news row and at the top of a topic. With
 * nothing to show (contest news) every text is cleared, so a reused row keeps nothing from the topic
 * it showed before. [titleView] is given apart: the two layouts name it differently.
 */
class TopicLinkedViewHolder(
        private val view: View,
        private val titleView: TextView,
        private val imageLoader: ImageLoader
) {

    fun bind(item: TopicLinkedViewModel?) {
        with(view) {
            imageView.visibleIf { item != null }
            if (item != null) imageLoader.setImageWithPlaceHolder(imageView, item.imageUrl)

            titleView.text = item?.name
            typeView.text = item?.typeText
            seasonView.text = item?.seasonText
            statusView.text = item?.statusText
        }
    }
}

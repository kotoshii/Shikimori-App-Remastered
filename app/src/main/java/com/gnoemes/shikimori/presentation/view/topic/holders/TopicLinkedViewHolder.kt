package com.gnoemes.shikimori.presentation.view.topic.holders

import android.widget.ImageView
import android.widget.TextView
import com.gnoemes.shikimori.entity.topic.presentation.TopicLinkedViewModel
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.visibleIf

/**
 * The title block, the same in the forum list's title-news row and at the top of a topic. With
 * nothing to show (contest news) every text is cleared, so a reused row keeps nothing from the topic
 * it showed before. The views are given one by one: the two layouts are two files, so two binding
 * classes (P7-T14), and they name the title differently.
 */
class TopicLinkedViewHolder(
        private val imageView: ImageView,
        private val titleView: TextView,
        private val typeView: TextView,
        private val seasonView: TextView,
        private val statusView: TextView,
        private val imageLoader: ImageLoader
) {

    fun bind(item: TopicLinkedViewModel?) {
        imageView.visibleIf { item != null }
        if (item != null) imageLoader.setImageWithPlaceHolder(imageView, item.imageUrl)

        titleView.text = item?.name
        typeView.text = item?.typeText
        seasonView.text = item?.seasonText
        statusView.text = item?.statusText
    }
}

package com.gnoemes.shikimori.presentation.view.topic.list.adapter

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemTopicBinding
import com.gnoemes.shikimori.entity.topic.presentation.TopicLinkedViewModel
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.entity.topic.presentation.TopicViewModel
import com.gnoemes.shikimori.presentation.view.topic.holders.TopicContentViewHolder
import com.gnoemes.shikimori.presentation.view.topic.holders.TopicUserViewHolder
import com.gnoemes.shikimori.utils.dimen
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.inflate
import com.gnoemes.shikimori.utils.visibleIf
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

class TopicAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val navigationCallback: (Type, Long) -> Unit,
        private val textListener: RichTextListener
) : AbsListItemAdapterDelegate<TopicViewModel, Any, TopicAdapterDelegate.ViewHolder>() {

    //fallback adapter
    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_topic))

    override fun onBindViewHolder(item: TopicViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemTopicBinding.bind(view)

        private lateinit var item: TopicViewModel

        private val userHolder by lazy { TopicUserViewHolder(binding.userLayout.root, imageLoader, navigationCallback) }
        private val topicHolder by lazy { TopicContentViewHolder(binding.topicLayout.root, imageLoader, textListener) }

        private val margin = itemView.context.dimen(R.dimen.margin_normal).toInt()

        init {
            binding.container.setOnClickListener { navigationCallback.invoke(Type.TOPIC, item.id) }
            binding.topicLayout.divider.gone()
            binding.linkedImageView.setOnClickListener {
                item.linked?.let { content ->
                    navigationCallback.invoke(content.type, content.id)
                }
            }

            (binding.topicLayout.titleView.layoutParams as ConstraintLayout.LayoutParams).apply { topMargin = 0 }
        }

        fun bind(item: TopicViewModel) {
            this.item = item
            setLinkedContent(item.linked)
            userHolder.bind(item.userData)
            topicHolder.bind(item.contentData)
            binding.commentView.text = item.commentsCount.toString()
        }

        private fun setLinkedContent(linked: TopicLinkedViewModel?) {
            with(itemView) {
                binding.linkedImageView.visibleIf { linked != null }

                if (linked !== null) {
                    imageLoader.setImageWithPlaceHolder(binding.linkedImageView, linked.imageUrl)
                    (binding.topicLayout.titleView.layoutParams as ConstraintLayout.LayoutParams).apply { leftMargin = margin }
                } else {
                    (binding.topicLayout.titleView.layoutParams as ConstraintLayout.LayoutParams).apply { leftMargin = 0 }
                }
            }
        }
    }
}
package com.gnoemes.shikimori.presentation.view.topic.details.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ItemCommentBinding
import com.gnoemes.shikimori.entity.comment.presentation.CommentViewModel
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextState
import com.gnoemes.shikimori.presentation.view.topic.holders.TopicUserViewHolder
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.inflate
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

/** A comment on the topic page; [stateOf] gives what the user opened in it, kept by the list (TopicAdapter). */
class CommentAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val navigationCallback: (Type, Long) -> Unit,
        private val textListener: RichTextListener,
        private val stateOf: (CommentViewModel) -> RichTextState
) : AbsListItemAdapterDelegate<CommentViewModel, Any, CommentAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean =
            item is CommentViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_comment))

    override fun onBindViewHolder(item: CommentViewModel, holder: ViewHolder, payloads: MutableList<Any>) {
        holder.bind(item)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val binding = ItemCommentBinding.bind(view)

        private lateinit var item: CommentViewModel

        private val userHolder by lazy { TopicUserViewHolder(binding.userLayout.root, imageLoader, navigationCallback) }

        init {
            binding.contentView.setup(imageLoader, textListener)
        }

        fun bind(item: CommentViewModel) {
            this.item = item
            userHolder.bind(item.userData)
            binding.contentView.setContentState(stateOf(item))

        }


    }
}
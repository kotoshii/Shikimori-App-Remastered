package com.gnoemes.shikimori.presentation.view.topic.details.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.LayoutTopicLinkedBinding
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextState
import com.gnoemes.shikimori.entity.topic.presentation.TopicLinkedViewModel
import com.gnoemes.shikimori.entity.topic.presentation.TopicPostEndRow
import com.gnoemes.shikimori.entity.topic.presentation.TopicPostPartRow
import com.gnoemes.shikimori.entity.topic.presentation.TopicTitleRow
import com.gnoemes.shikimori.entity.topic.presentation.TopicUserViewModel
import com.gnoemes.shikimori.presentation.view.common.widget.shikimori.RichTextRenderer
import com.gnoemes.shikimori.presentation.view.topic.holders.TopicLinkedViewHolder
import com.gnoemes.shikimori.presentation.view.topic.holders.TopicUserViewHolder
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.inflate
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

/*
 * The post's rows on the topic page (TopicAdapter): its author, its title, its parts, the line under it, and
 * the title block. The author and the title block are the views the page showed above its comments before
 * P8-T4's one-list step, with their holders.
 */

class TopicAuthorAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val navigationCallback: (Type, Long) -> Unit
) : AbsListItemAdapterDelegate<TopicUserViewModel, Any, TopicAuthorAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = item is TopicUserViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = ViewHolder(parent.inflate(R.layout.layout_topic_user))

    override fun onBindViewHolder(item: TopicUserViewModel, holder: ViewHolder, payloads: MutableList<Any>) =
            holder.user.bind(item)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val user = TopicUserViewHolder(view, imageLoader, navigationCallback)
    }
}

class TopicTitleAdapterDelegate : AbsListItemAdapterDelegate<TopicTitleRow, Any, TopicTitleAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = item is TopicTitleRow

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = ViewHolder(parent.inflate(R.layout.item_topic_post_title))

    override fun onBindViewHolder(item: TopicTitleRow, holder: ViewHolder, payloads: MutableList<Any>) {
        (holder.itemView as TextView).text = item.title
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

/**
 * One top-level part of the post, built when it scrolls in. One renderer serves every part of the post, so they
 * share the zoom gallery and what is opened ([stateOf] gives the post's state, kept by the adapter).
 */
class TopicPostPartAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val textListener: RichTextListener,
        private val stateOf: (RichText) -> RichTextState
) : AbsListItemAdapterDelegate<TopicPostPartRow, Any, TopicPostPartAdapterDelegate.ViewHolder>() {

    private var renderer: RichTextRenderer? = null

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = item is TopicPostPartRow

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = ViewHolder(parent.inflate(R.layout.item_topic_post_part))

    override fun onBindViewHolder(item: TopicPostPartRow, holder: ViewHolder, payloads: MutableList<Any>) {
        val root = holder.itemView as LinearLayout
        val state = stateOf(item.text)
        val renderer = renderer?.takeIf { it.state === state }
                ?: RichTextRenderer(root.context, imageLoader, textListener, state).also { renderer = it }
        root.removeAllViews()
        val shown = renderer.renderBlock(item.index, root)
        //the content view's gap between parts; none above the first, and none for a part that shows nothing,
        //so its row has no height
        root.setPadding(root.paddingLeft, if (shown && item.index > 0) renderer.blockGap else 0, root.paddingRight, 0)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

class TopicPostEndAdapterDelegate : AbsListItemAdapterDelegate<TopicPostEndRow, Any, TopicPostEndAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = item === TopicPostEndRow

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = ViewHolder(parent.inflate(R.layout.item_topic_post_end))

    override fun onBindViewHolder(item: TopicPostEndRow, holder: ViewHolder, payloads: MutableList<Any>) = Unit

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

/** The title block (`TopicLinkedViewHolder`, the same as in the forum's rows); a tap opens the title. */
class TopicTitleBlockAdapterDelegate(
        private val imageLoader: ImageLoader,
        private val navigationCallback: (Type, Long) -> Unit
) : AbsListItemAdapterDelegate<TopicLinkedViewModel, Any, TopicTitleBlockAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = item is TopicLinkedViewModel

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = ViewHolder(parent.inflate(R.layout.layout_topic_linked))

    override fun onBindViewHolder(item: TopicLinkedViewModel, holder: ViewHolder, payloads: MutableList<Any>) =
            holder.bind(item)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val binding = LayoutTopicLinkedBinding.bind(view)
        private val linked = TopicLinkedViewHolder(binding.imageView, binding.linkedTitleView, binding.typeView,
                binding.seasonView, binding.statusView, imageLoader)
        private lateinit var item: TopicLinkedViewModel

        init {
            view.setOnClickListener { navigationCallback(item.type, item.id) }
        }

        fun bind(item: TopicLinkedViewModel) {
            this.item = item
            linked.bind(item)
        }
    }
}

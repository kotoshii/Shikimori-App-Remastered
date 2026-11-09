package com.gnoemes.shikimori.presentation.view.topic.details.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.topic.presentation.CommentsHeaderRow
import com.gnoemes.shikimori.entity.topic.presentation.CommentsMoreRow
import com.gnoemes.shikimori.utils.inflate
import com.hannesdorfmann.adapterdelegates4.AbsListItemAdapterDelegate

/* The rows above the comments on the topic page (TopicAdapter): «Комментарии (N):» and «Загрузить еще». */

class CommentsHeaderAdapterDelegate : AbsListItemAdapterDelegate<CommentsHeaderRow, Any, CommentsHeaderAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = item is CommentsHeaderRow

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = ViewHolder(parent.inflate(R.layout.item_comments_header))

    override fun onBindViewHolder(item: CommentsHeaderRow, holder: ViewHolder, payloads: MutableList<Any>) {
        val view = holder.itemView as TextView
        view.text = view.context.getString(R.string.common_comments) + " (${item.count}):"
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

/** «Загрузить еще …»: a tap asks for the next page of older comments, which come under it. */
class CommentsMoreAdapterDelegate(
        private val onClick: () -> Unit
) : AbsListItemAdapterDelegate<CommentsMoreRow, Any, CommentsMoreAdapterDelegate.ViewHolder>() {

    override fun isForViewType(item: Any, items: MutableList<Any>, position: Int): Boolean = item is CommentsMoreRow

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder =
            ViewHolder(parent.inflate(R.layout.item_comments_more)).also { holder -> holder.itemView.setOnClickListener { onClick() } }

    override fun onBindViewHolder(item: CommentsMoreRow, holder: ViewHolder, payloads: MutableList<Any>) {
        (holder.itemView as TextView).text = item.text
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}

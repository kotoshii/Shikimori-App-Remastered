package com.gnoemes.shikimori.presentation.view.topic.details.adapter

import com.gnoemes.shikimori.entity.comment.presentation.CommentViewModel
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.PlaceholderItem
import com.gnoemes.shikimori.entity.common.presentation.ProgressItem
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextState
import com.gnoemes.shikimori.entity.topic.presentation.TopicPostPartRow
import com.gnoemes.shikimori.presentation.view.common.adapter.BaseAdapter
import com.gnoemes.shikimori.presentation.view.common.adapter.ProgressAdapterDelegate
import com.gnoemes.shikimori.utils.images.ImageLoader

/**
 * The whole topic page as one list, in reading order: the post (author, title, its parts, the title block),
 * then the comments' header, a spinner or «Загрузить еще», and the comments, oldest first. Only the rows on
 * screen are built; until P8-T4 the comments were a list inside a scroll view, which built every row at once.
 * The rows: TopicPageRows.kt.
 */
class TopicAdapter(
        imageLoader: ImageLoader,
        navigationCallback: (Type, Long) -> Unit,
        textListener: RichTextListener,
        onMoreClicked: () -> Unit
) : BaseAdapter<Any>() {

    //what the user opened in each text, for as long as this adapter lives (the fragment keeps it while its view
    //is rebuilt): a row that leaves the screen is reused for another, and its text is built again from this
    private val commentStates = HashMap<Long, RichTextState>()
    private var shownPostState: RichTextState? = null

    init {
        delegatesManager.apply {
            addDelegate(TopicAuthorAdapterDelegate(imageLoader, navigationCallback))
            addDelegate(TopicTitleAdapterDelegate())
            addDelegate(TopicPostPartAdapterDelegate(imageLoader, textListener, ::postState))
            addDelegate(TopicPostEndAdapterDelegate())
            addDelegate(TopicTitleBlockAdapterDelegate(imageLoader, navigationCallback))
            addDelegate(CommentsHeaderAdapterDelegate())
            addDelegate(CommentsMoreAdapterDelegate(onMoreClicked))
            addDelegate(ProgressAdapterDelegate())
            addDelegate(CommentPlaceholderAdapterDelegate())
            addDelegate(CommentAdapterDelegate(imageLoader, navigationCallback, textListener, ::commentState))
        }
    }

    //a state belongs to one text: a text read again (another instance) starts closed
    private fun postState(text: RichText): RichTextState =
            shownPostState?.takeIf { it.text === text } ?: RichTextState(text).also { shownPostState = it }

    private fun commentState(comment: CommentViewModel): RichTextState =
            commentStates[comment.id]?.takeIf { it.text === comment.content }
                    ?: RichTextState(comment.content).also { commentStates[comment.id] = it }

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean = when {
        oldItem is CommentViewModel && newItem is CommentViewModel -> oldItem.id == newItem.id
        oldItem is TopicPostPartRow && newItem is TopicPostPartRow -> oldItem.index == newItem.index
        //every other row is one of a kind
        else -> oldItem.javaClass == newItem.javaClass
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean = when (oldItem) {
        is ProgressItem, is PlaceholderItem -> true
        //the same instances while nothing changed, so mostly an identity check, not a deep compare of texts
        else -> oldItem == newItem
    }
}

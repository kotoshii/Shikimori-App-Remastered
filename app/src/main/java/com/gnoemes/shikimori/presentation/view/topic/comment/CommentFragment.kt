package com.gnoemes.shikimori.presentation.view.topic.comment

import android.content.Context
import android.os.Bundle
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.DialogCommentBinding
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.comment.presentation.CommentSheetState
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.richtext.Image
import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.presentation.presenter.topic.comment.CommentPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetInjectionDialogFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragment
import com.gnoemes.shikimori.presentation.view.topic.holders.TopicUserViewHolder
import com.gnoemes.shikimori.utils.addBackButton
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.visibleIf
import com.gnoemes.shikimori.utils.withArgs
import javax.inject.Inject

/**
 * One comment, over the screen whose text mentions it (P8-T4, Q11): its author, its text, and
 * «Открыть обсуждение» when it is in another topic («Открыть профиль» for a comment on a profile's wall).
 * It loads over the network, so it opens with a spinner (CODE_CONVENTIONS §7). Its taps go to the screen
 * under it ([BaseFragment.textListener]), which opens pages in its own tab; the sheet closes when a page
 * opens there.
 */
class CommentFragment : BaseBottomSheetInjectionDialogFragment<CommentPresenter, CommentView>(), CommentView {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var commentPresenter: CommentPresenter

    @ProvidePresenter
    fun providePresenter(): CommentPresenter = presenterProvider.get().apply {
        id = arguments?.getLong(ARGUMENT_ID, Constants.NO_ID) ?: Constants.NO_ID
        topicOnScreen = arguments?.getLong(ARGUMENT_TOPIC_ON_SCREEN, Constants.NO_ID)?.takeIf { it != Constants.NO_ID }
    }

    companion object {
        const val TAG = "CommentFragment"

        fun newInstance(id: Long, topicOnScreen: Long?) = CommentFragment().withArgs {
            putLong(ARGUMENT_ID, id)
            topicOnScreen?.let { putLong(ARGUMENT_TOPIC_ON_SCREEN, it) }
        }

        private const val ARGUMENT_ID = "ARGUMENT_COMMENT_ID"
        private const val ARGUMENT_TOPIC_ON_SCREEN = "ARGUMENT_TOPIC_ON_SCREEN"
    }

    override val presenter: CommentPresenter
        get() = commentPresenter

    private val binding by viewBinding(DialogCommentBinding::bind)

    private lateinit var userHolder: TopicUserViewHolder

    private val host: RichTextListener?
        get() = (parentFragment as? BaseFragment<*, *>)?.textListener

    private val listener = object : RichTextListener {
        override fun onLinkClicked(target: LinkTarget) {
            host?.onLinkClicked(target)
            //a page opens in the tab under the sheet, which would stay over it; a comment opens one more
            //sheet, and a web page leaves the app
            val opensPage = (target is LinkTarget.Screen && target.type != Type.COMMENT) || target is LinkTarget.UserByNickname
            if (opensPage) dismiss()
        }

        override fun onImageClicked(images: List<Image>, index: Int) {
            host?.onImageClicked(images, index)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        //as tall as the comment, not the whole screen
        peekHeight = context.dimenAttr(android.R.attr.actionBarSize)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(wrapper.toolbar) {
            setTitle(R.string.comment_title)
            addBackButton(R.drawable.ic_close) { dismiss() }
        }

        userHolder = TopicUserViewHolder(binding.userLayout.root, imageLoader) { type, id -> listener.onLinkClicked(LinkTarget.Screen(type, id)) }
        binding.contentView.setup(imageLoader, listener)

        with(binding.networkErrorView) {
            setText(R.string.common_error_message_without_pull)
            showButton()
            callback = { presenter.onRetry() }
        }
    }

    override fun setState(state: CommentSheetState) {
        binding.progressBar.visibleIf { state is CommentSheetState.Loading }
        binding.networkErrorView.visibleIf { state is CommentSheetState.Failed }
        binding.messageView.visibleIf { state is CommentSheetState.Deleted }
        binding.commentLayout.visibleIf { state is CommentSheetState.Shown }
        if (state !is CommentSheetState.Shown) return

        userHolder.bind(state.comment.userData)
        binding.contentView.setContent(state.comment.content)
        val open = state.open
        binding.openButton.visibleIf { open != null }
        if (open != null) {
            binding.openButton.setText(if (open.type == Type.USER) R.string.comment_open_profile else R.string.comment_open_topic)
            binding.openButton.setOnClickListener { listener.onLinkClicked(open) }
        }
    }

    override fun getDialogLayout(): Int = R.layout.dialog_comment
}

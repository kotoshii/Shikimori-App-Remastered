package com.gnoemes.shikimori.presentation.view.topic.details

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentTopicBinding
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.comment.presentation.CommentViewModel
import com.gnoemes.shikimori.entity.topic.presentation.TopicContentViewModel
import com.gnoemes.shikimori.entity.topic.presentation.TopicLinkedViewModel
import com.gnoemes.shikimori.entity.topic.presentation.TopicUserViewModel
import com.gnoemes.shikimori.presentation.presenter.topic.details.TopicPresenter
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter
import com.gnoemes.shikimori.presentation.view.base.fragment.BasePaginationFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.topic.details.adapter.CommentsAdapter
import com.gnoemes.shikimori.presentation.view.topic.holders.TopicContentViewHolder
import com.gnoemes.shikimori.presentation.view.topic.holders.TopicLinkedViewHolder
import com.gnoemes.shikimori.presentation.view.topic.holders.TopicUserViewHolder
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import javax.inject.Inject

class TopicFragment : BasePaginationFragment<CommentViewModel, TopicPresenter, TopicView>(), TopicView {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var topicPresenter: TopicPresenter

    @ProvidePresenter
    fun providePresenter(): TopicPresenter {
        return presenterProvider.get().apply {
            localRouter = (parentFragment as RouterProvider).localRouter
            id = arguments?.getLong(AppExtras.ARGUMENT_TOPIC_ID) ?: Constants.NO_ID
        }
    }

    companion object {
        fun newInstance(id: Long) = TopicFragment().withArgs { putLong(AppExtras.ARGUMENT_TOPIC_ID, id) }
        private const val PREVIOUS_KEY = "PREVIOUS_KEY"
    }

    private val commentsAdapter by lazy { CommentsAdapter(imageLoader, getPresenter()::onContentClicked).apply { if (!hasObservers()) setHasStableIds(true) } }

    private lateinit var userHolder: TopicUserViewHolder
    private lateinit var contentHolder: TopicContentViewHolder
    private lateinit var linkedHolder: TopicLinkedViewHolder

    private var isPrevious: Boolean = false

    override val adapter: BasePaginationAdapter
        get() = commentsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            isPrevious = it.getBoolean(PREVIOUS_KEY, false)
        }

        ViewCompat.setNestedScrollingEnabled(binding.scrollView, false)
        userHolder = TopicUserViewHolder(binding.userLayout.root, imageLoader, getPresenter()::onContentClicked)
        contentHolder = TopicContentViewHolder(binding.topicLayout.root, getPresenter()::onContentClicked)
        //the title block's views, found inside it: the same ones as before (its title's id is in no other layout)
        val linkedViews = binding.linkedLayout
        linkedHolder = TopicLinkedViewHolder(linkedViews.imageView, linkedViews.linkedTitleView, linkedViews.typeView,
                linkedViews.seasonView, linkedViews.statusView, imageLoader)

        screenToolbar?.addBackButton { getPresenter().onBackPressed() }
        screenToolbar?.title = null

        with(binding.commentsLayout.recyclerView) {
            adapter = this@TopicFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
            itemAnimator = null
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.commentsLayout.commentsMore.setOnClickListener { isPrevious = false; getPresenter().loadNextPage() }
        binding.commentsLayout.commentsBefore.setOnClickListener { isPrevious = true; getPresenter().onPreviousClicked() }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(PREVIOUS_KEY, isPrevious)
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): TopicPresenter = topicPresenter

    private val binding by viewBinding(FragmentTopicBinding::bind)

    override fun getFragmentLayout(): Int = R.layout.fragment_topic

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showData(data: List<Any>) {
        commentsAdapter.bindItems(data, isPrevious)
    }

    override fun setUserData(item: TopicUserViewModel) {
        userHolder.bind(item)
    }

    override fun setContentData(item: TopicContentViewModel) {
        contentHolder.bind(item)
    }

    override fun setCommentsText(text: String?) {
        binding.commentsLayout.commentsMore.text = text
    }

    override fun setLinkedContent(linked: TopicLinkedViewModel?) {
        binding.linkedLayout.root.visibleIf { linked != null }
        binding.linkedLayout.root.setOnClickListener { linked?.let { getPresenter().onContentClicked(it.type, it.id) } }
        linkedHolder.bind(linked)
    }

    override fun showCommentsLoading(show: Boolean) {
        binding.commentsLayout.commentProgress.root.visibleIf { show }
        binding.commentsLayout.recyclerView.visibleIf { !show }
    }

    override fun onShowLoading() {
        screenProgress?.visible()
    }

    override fun onHideLoading() {
        screenProgress?.gone()
    }

    override fun setCommentsCount(count: Long) {
        context?.let {
            val text = it.getString(R.string.common_comments) + " ($count):"
            binding.commentsLayout.commentTitleView.text = text
        }
    }

    override fun showEmptyView() {
        commentsAdapter.showEmptyView()
    }

    override fun hideEmptyView() {
        commentsAdapter.hideEmptyView()
    }

    override fun showContent(show: Boolean) {
        binding.scrollView.visibleIf { show }
    }

    override fun showCommentsMore(show: Boolean) {
        binding.commentsLayout.commentsMore.visibleIf { show }
    }

    override fun showPreviousComments(show: Boolean) {
        binding.commentsLayout.commentsBefore.visibleIf { show }
    }

}
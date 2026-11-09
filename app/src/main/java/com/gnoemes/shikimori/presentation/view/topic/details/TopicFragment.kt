package com.gnoemes.shikimori.presentation.view.topic.details

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentTopicBinding
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.presentation.presenter.topic.details.TopicPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.topic.details.adapter.TopicAdapter
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.widgets.NoAutoScrollLayoutManager
import javax.inject.Inject

/** A topic: its post and its comments, as one list (TopicAdapter, rows from TopicPresenter). */
class TopicFragment : BaseFragment<TopicPresenter, TopicView>(), TopicView {

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

        //rows from the post's top: further down, scrollToTop jumps here first
        private const val NEAR_TOP = 6
    }

    //kept with the fragment, not its view: what the user opened in each text stays while the view is rebuilt
    //(a page opened over this one, then back)
    private val topicAdapter by lazy {
        TopicAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter(), getPresenter()::onMoreClicked)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        screenToolbar?.addBackButton { getPresenter().onBackPressed() }
        screenToolbar?.title = null

        with(binding.recyclerView) {
            adapter = topicAdapter
            layoutManager = NoAutoScrollLayoutManager(context)
            itemAnimator = null
            //off, as it was for the scroll view this list replaced
            isNestedScrollingEnabled = false
        }
    }

    //the adapter outlives this view: let go of the list, or the adapter keeps the old view tree
    override fun onDestroyView() {
        binding.recyclerView.adapter = null
        super.onDestroyView()
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

    override fun showRows(rows: List<Any>) {
        topicAdapter.bindItems(rows)
    }

    //from far down a smooth scroll over every row would take seconds: a jump to near the top first, laid out
    //before the smooth part starts (a posted action runs after the layout the jump asked for)
    override fun scrollToTop() {
        val list = binding.recyclerView
        val first = (list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (first > NEAR_TOP) {
            list.scrollToPosition(NEAR_TOP)
            list.post { list.smoothScrollToPosition(0) }
        } else list.smoothScrollToPosition(0)
    }

    override fun showContent(show: Boolean) {
        binding.recyclerView.visibleIf { show }
    }

}

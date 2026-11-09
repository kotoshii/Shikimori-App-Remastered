package com.gnoemes.shikimori.presentation.presenter.topic.details

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.comment.CommentInteractor
import com.gnoemes.shikimori.domain.topic.TopicInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.comment.domain.CommentableType
import com.gnoemes.shikimori.entity.comment.presentation.CommentViewModel
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget
import com.gnoemes.shikimori.entity.topic.domain.Topic
import com.gnoemes.shikimori.entity.topic.presentation.TopicViewModel
import com.gnoemes.shikimori.presentation.presenter.base.BasePaginationPresenter
import com.gnoemes.shikimori.presentation.presenter.topic.converter.TopicViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.topic.provider.TopicResourceProvider
import com.gnoemes.shikimori.presentation.view.topic.details.TopicView
import com.gnoemes.shikimori.presentation.presenter.topic.converter.CommentViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.base.appendLoadingLogic
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@InjectViewState
class TopicPresenter @Inject constructor(
        private val interactor: TopicInteractor,
        private val commentInteractor: CommentInteractor,
        private val converter: TopicViewModelConverter,
        private val resourceProvider: TopicResourceProvider,
        private val commentConverter: CommentViewModelConverter
) : BasePaginationPresenter<CommentViewModel, TopicView>() {

    var id: Long = Constants.NO_ID
    private val comments = mutableListOf<CommentViewModel>()
    private var lastPage = 1

    private lateinit var topic: Topic

    //one topic load at a time, closed with the screen: it was never disposed (T5-17)
    private val topicLoad = loadSlot()

    override fun initData() {
        super.initData()

        loadTopic()
        viewState.showCommentsMore(false)
    }

    //reading a long post's HTML takes a while, so it runs off the main thread, and before the
    //loading logic so the spinner stays up until the text is ready
    private fun loadTopic() = topicLoad.start {
        interactor.getDetails(id)
                .observeOn(Schedulers.computation())
                .map { it to converter.convertTopic(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .appendLoadingLogic(viewState)
                .doOnSuccess { (loaded, _) -> topic = loaded }
                //the count needs the post's total: comments that came first counted 0 and hid
                //«Загрузить еще» for good (P7-T5 Q96)
                .doOnSuccess { if (comments.isNotEmpty()) countComments(lastPage, Constants.COMMENTS_LIMIT) }
                .doOnSuccess { (loaded, _) -> viewState.setTitle(loaded.forumName) }
                .subscribe({ (_, item) -> setData(item) }, this::processErrors)
    }

    override val shownTopicId: Long?
        get() = id

    //a mention of this very topic (a `[topic]` tag, a quote of its post): its top, not the topic again
    //(the user, 2026-11-09, P8-T4 Q15)
    override fun onLinkClicked(target: LinkTarget) {
        if (target is LinkTarget.Screen && target.type == Type.TOPIC && target.id == id) viewState.scrollToTop()
        else super.onLinkClicked(target)
    }

    override val galleryName: String
        get() = if (::topic.isInitialized) topic.title else ""

    private fun setData(item: TopicViewModel) {
        viewState.setUserData(item.userData)
        viewState.setContentData(item.contentData)
        viewState.setCommentsCount(item.commentsCount)
        viewState.setLinkedContent(item.linked)
    }

    fun onPreviousClicked() {
        if (comments.isNotEmpty()) {
            viewState.showData(comments.take(lastPage * Constants.COMMENTS_LIMIT))
            viewState.showPreviousComments(false)
        }
    }

    override fun getPaginatorRequestFactory(): (Int) -> Single<Page<CommentViewModel>> {
        return { page: Int ->
            //the comments' HTML is read on a background thread (P8-T4 plan §3.2)
            commentInteractor.getList(id, CommentableType.TOPIC, page)
                    .observeOn(Schedulers.computation())
                    .map { it.map(commentConverter::apply) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess { countComments(page, Constants.COMMENTS_LIMIT) }
                    .doOnSuccess { lastPage = page }
        }
    }

    private fun countComments(page: Int, limit: Int) {
        val commentsSize = if (::topic.isInitialized) topic.commentsCount else 0L

        if (commentsSize > 0L) viewState.showCommentsMore(true)


        val commentsLoaded = page * limit
        if (commentsLoaded >= commentsSize) viewState.showCommentsMore(false)
        else {
            // configure message for load more comments
            // Load more $limit from $size
            // or
            // Load more $last
            fun configureMore() {
                val diff = (commentsSize - commentsLoaded).toInt().let { if (it - limit <= 0) null else it }
                val load = if (diff == null) (commentsSize - commentsLoaded).toInt() else limit
                val text = resourceProvider.getCommentsMoreText(load, diff)

                viewState.showCommentsMore(true)
                viewState.setCommentsText(text)
            }

            configureMore()
        }

        viewState.showPreviousComments(page > 1)
    }

    //the post owns the screen: the comments only fill their own list, and their errors are a message.
    //An empty or failed comments load used to hide the whole post (BUGS #38)
    override fun showData(show: Boolean, data: List<CommentViewModel>) {
        if (show) viewState.showData(data)
        if (data.isNotEmpty()) {
            comments.clear()
            comments.addAll(data)
        }
    }

    override fun showEmptyView(show: Boolean) {
        if (show) viewState.showEmptyView()
        else viewState.hideEmptyView()
    }

    override fun showEmptyError(show: Boolean, throwable: Throwable?) {
        if (show && throwable != null) showError(throwable)
    }

    override fun showError(throwable: Throwable) = showErrorText(throwable)

    //«Загрузить еще…» stays the way to ask for the page again
    override fun showPageError(show: Boolean, throwable: Throwable?) {
        throwable?.let { showError(it) }
    }

    override fun showEmptyProgress(show: Boolean) {
        viewState.showCommentsLoading(show)
    }
}
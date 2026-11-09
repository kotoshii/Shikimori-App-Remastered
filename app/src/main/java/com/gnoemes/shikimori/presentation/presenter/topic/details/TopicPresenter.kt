package com.gnoemes.shikimori.presentation.presenter.topic.details

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.comment.CommentInteractor
import com.gnoemes.shikimori.domain.topic.TopicInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.comment.domain.CommentableType
import com.gnoemes.shikimori.entity.comment.presentation.CommentViewModel
import com.gnoemes.shikimori.entity.common.domain.Page
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.PlaceholderItem
import com.gnoemes.shikimori.entity.common.presentation.ProgressItem
import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget
import com.gnoemes.shikimori.entity.topic.domain.Topic
import com.gnoemes.shikimori.entity.topic.presentation.CommentsHeaderRow
import com.gnoemes.shikimori.entity.topic.presentation.CommentsMoreRow
import com.gnoemes.shikimori.entity.topic.presentation.TopicPostEndRow
import com.gnoemes.shikimori.entity.topic.presentation.TopicPostPartRow
import com.gnoemes.shikimori.entity.topic.presentation.TopicTitleRow
import com.gnoemes.shikimori.entity.topic.presentation.TopicViewModel
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.presenter.base.appendLoadingLogic
import com.gnoemes.shikimori.presentation.presenter.common.paginator.PageOffsetPaginator
import com.gnoemes.shikimori.presentation.presenter.common.paginator.ViewController
import com.gnoemes.shikimori.presentation.presenter.topic.converter.CommentViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.topic.converter.TopicViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.topic.provider.TopicResourceProvider
import com.gnoemes.shikimori.presentation.view.topic.details.TopicView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * A topic: its post, then its comments, as one list whose rows this presenter builds ([showRows]). The comments
 * come a page at a time, newest first; «Загрузить еще» adds the next older page under itself and every page
 * loaded stays, as on the site (P8-T4, the one-list step, Q30).
 *
 * Not a paged-list presenter (`BasePaginationPresenter`): that base puts a spinner at the end of its list and
 * loads pages as the list scrolls, which does not fit a post with «Загрузить еще» above its comments. It uses
 * the same page loader, [PageOffsetPaginator].
 */
@InjectViewState
class TopicPresenter @Inject constructor(
        private val interactor: TopicInteractor,
        private val commentInteractor: CommentInteractor,
        private val converter: TopicViewModelConverter,
        private val resourceProvider: TopicResourceProvider,
        private val commentConverter: CommentViewModelConverter
) : BaseNetworkPresenter<TopicView>(), ViewController<CommentViewModel> {

    var id: Long = Constants.NO_ID

    private lateinit var topic: Topic
    //the post, read: the page shows nothing until it is here, so the post owns the screen
    private var post: TopicViewModel? = null

    //every comment loaded, newest first as the api sends them; the page shows them oldest first
    private var comments: List<CommentViewModel> = emptyList()
    private var lastPage = 1
    private var isFirstPageLoading = false
    private var isPageLoading = false
    private var hasNoComments = false

    private val progressRow = ProgressItem()
    private val placeholderRow = PlaceholderItem()

    private var paginator: PageOffsetPaginator<CommentViewModel>? = null

    //one topic load at a time, closed with the screen: it was never disposed (T5-17)
    private val topicLoad = loadSlot()

    //also «Повторить» on the error view (onRetry): the topic again, and the comments from their first page
    override fun initData() {
        super.initData()

        loadTopic()
        val paginator = paginator ?: PageOffsetPaginator(::loadComments, this).also { paginator = it }
        paginator.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        paginator?.release()
        paginator = null
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
                .doOnSuccess { (loaded, _) -> viewState.setTitle(loaded.forumName) }
                .subscribe({ (_, item) -> post = item; showRows() }, this::processErrors)
    }

    //the comments' HTML is read on a background thread (P8-T4 plan §3.2)
    private fun loadComments(page: Int): Single<Page<CommentViewModel>> =
            commentInteractor.getList(id, CommentableType.TOPIC, page)
                    .observeOn(Schedulers.computation())
                    .map { it.map(commentConverter::apply) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSuccess { lastPage = page }

    fun onMoreClicked() {
        paginator?.loadNewPage()
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

    /**
     * The whole page in reading order: the post's author, title and parts (one row each, so a long post is
     * built as it scrolls in: P8-T4 Q29), the title block, «Комментарии (N):», then a spinner while a page
     * loads, else «Загрузить еще» while older comments are left, then the comments, oldest first.
     */
    private fun showRows() {
        val post = post ?: return
        val text = post.contentData.content
        val rows = ArrayList<Any>(text.blocks.size + comments.size + 8)
        rows.add(post.userData)
        rows.add(TopicTitleRow(post.contentData.title))
        text.blocks.indices.mapTo(rows) { TopicPostPartRow(text, it) }
        rows.add(TopicPostEndRow)
        post.linked?.let { rows.add(it) }
        rows.add(CommentsHeaderRow(post.commentsCount))
        when {
            isFirstPageLoading || isPageLoading -> rows.add(progressRow)
            comments.isNotEmpty() -> moreText(post.commentsCount)?.let { rows.add(CommentsMoreRow(it)) }
        }
        if (hasNoComments) rows.add(placeholderRow)
        rows.addAll(comments.asReversed())
        viewState.showRows(rows)
    }

    /**
     * «Загрузить еще 20 из N», or «Загрузить последние N»; null when every comment is loaded. Counted from the
     * post's total, so only once the post is here: comments that came first counted 0 and hid it for good
     * (P7-T5 Q96).
     */
    private fun moreText(total: Long): String? {
        val limit = Constants.COMMENTS_LIMIT
        val left = total - lastPage * limit
        if (left <= 0) return null
        return if (left > limit) resourceProvider.getCommentsMoreText(limit, left.toInt())
        else resourceProvider.getCommentsMoreText(left.toInt(), null)
    }

    ///////////////////////////////////////////////////////////////////////////
    // THE COMMENTS' PAGES (PageOffsetPaginator)
    ///////////////////////////////////////////////////////////////////////////

    //the post owns the screen: the comments only fill their own rows, and their errors are a message.
    //An empty or failed comments load used to hide the whole post (BUGS #38)
    override fun showData(show: Boolean, data: List<CommentViewModel>) {
        if (show) comments = data.toList()
        showRows()
    }

    override fun showEmptyProgress(show: Boolean) {
        isFirstPageLoading = show
        showRows()
    }

    override fun showPageProgress(show: Boolean) {
        isPageLoading = show
        showRows()
    }

    override fun showEmptyView(show: Boolean) {
        hasNoComments = show
        showRows()
    }

    //the comments again from their first page («Повторить» on the error view): the screen's spinner
    override fun showRefreshProgress(show: Boolean) {
        if (show) viewState.onShowLoading() else viewState.onHideLoading()
    }

    override fun showEmptyError(show: Boolean, throwable: Throwable?) {
        if (show && throwable != null) showError(throwable)
    }

    override fun showError(throwable: Throwable) = showErrorText(throwable)

    //«Загрузить еще…» comes back as the way to ask for the page again
    override fun showPageError(show: Boolean, throwable: Throwable?) {
        throwable?.let { showError(it) }
    }
}

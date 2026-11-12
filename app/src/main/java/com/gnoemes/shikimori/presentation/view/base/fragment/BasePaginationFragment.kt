package com.gnoemes.shikimori.presentation.view.base.fragment

import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.presentation.presenter.base.BasePaginationPresenter
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter
import com.gnoemes.shikimori.utils.hideRefresh
import com.gnoemes.shikimori.utils.showRefresh
import com.gnoemes.shikimori.utils.visible
import com.gnoemes.shikimori.utils.visibleIf

abstract class BasePaginationFragment<Items : Any, Presenter : BasePaginationPresenter<Items, View>, View : BasePaginationView> : BaseFragment<Presenter, View>(), BasePaginationView {

    abstract val adapter: BasePaginationAdapter

    //the list, by id as the synthetics found it: layout_default_list on most screens, the comments on a topic
    //(no refresh there), so no one binding class has them
    private val refreshView: SwipeRefreshLayout? get() = screenView(R.id.refreshLayout)
    private val listView: RecyclerView? get() = screenView(R.id.recyclerView)

    protected open val nextPageListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            askNextPageNearEnd(recyclerView)
        }
    }

    //the same check after every layout of the list too: a list whose rows all fit on the screen cannot scroll,
    //so the scroll listener alone never asked for more - a tablet with small catalog cards stopped at 100 of
    //them, its swipes only sliding the bar away (P10-T3 Q24). A layout also follows the spinner row's posted
    //removal, which the check waits for
    private val nextPageOnLayout = ViewTreeObserver.OnGlobalLayoutListener { listView?.let(::askNextPageNearEnd) }

    //the end of the list within half a page of the screen's last row: the next page. Not while the spinner or
    //«Повторить» ends the list: RecyclerView calls the scroll listener after a layout too, with no scroll, so
    //a failed page would be asked for again and again. Not for a list with nothing on screen (hidden, or empty)
    private fun askNextPageNearEnd(recyclerView: RecyclerView) {
        if (recyclerView.childCount == 0) return
        val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val visibleItemPosition = manager.findLastCompletelyVisibleItemPosition() + Constants.DEFAULT_LIMIT / 2
        if (!adapter.isProgress() && !adapter.isPageError() && visibleItemPosition >= manager.itemCount) {
            getPresenter().loadNextPage()
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshView?.setOnRefreshListener { getPresenter().onRefresh() }
        adapter.onRetryPage = { getPresenter().loadNextPage() }
        listView?.viewTreeObserver?.addOnGlobalLayoutListener(nextPageOnLayout)
    }

    override fun onDestroyView() {
        listView?.removeOnScrollListener(nextPageListener)
        listView?.viewTreeObserver?.removeOnGlobalLayoutListener(nextPageOnLayout)
        super.onDestroyView()
    }

    override fun showData(data: List<Any>) {
        adapter.bindItems(data)
    }

    override fun showContent(show: Boolean) = listView?.visibleIf { show } ?: Unit
    override fun onShowLoading() = refreshView?.showRefresh() ?: Unit
    override fun onHideLoading() = refreshView?.hideRefresh() ?: Unit
    override fun showPageLoading() = postViewAction { adapter.showProgress(true) }
    override fun hidePageLoading() = postViewAction { adapter.showProgress(false) }
    //posted as the spinner is, so the two keep their order
    override fun showPageError() = postViewAction { adapter.showPageError(true) }
    override fun hidePageError() = postViewAction { adapter.showPageError(false) }
    override fun onAllData() = Unit

    override fun showHiddenListView(guest: Boolean) {
        screenEmptyView?.setText(if (guest) R.string.list_hidden_guest else R.string.list_hidden)
        screenEmptyView?.visible()
    }
}
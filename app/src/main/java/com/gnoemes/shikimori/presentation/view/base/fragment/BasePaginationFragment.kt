package com.gnoemes.shikimori.presentation.view.base.fragment

import android.os.Bundle
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

            val manager = (recyclerView.layoutManager as LinearLayoutManager)
            val visibleItemPosition = manager.findLastCompletelyVisibleItemPosition() + Constants.DEFAULT_LIMIT / 2
            val itemCount = manager.itemCount

            //not while «Повторить» ends the list: RecyclerView calls this after a layout too, with no
            //scroll, so a failed page would be asked for again and again
            if (!adapter.isProgress() && !adapter.isPageError() && visibleItemPosition >= itemCount) {
                getPresenter().loadNextPage()
            }
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refreshView?.setOnRefreshListener { getPresenter().onRefresh() }
        adapter.onRetryPage = { getPresenter().loadNextPage() }
    }

    override fun onDestroyView() {
        listView?.removeOnScrollListener(nextPageListener)
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
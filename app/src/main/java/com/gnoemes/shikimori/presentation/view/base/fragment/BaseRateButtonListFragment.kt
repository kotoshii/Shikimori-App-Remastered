package com.gnoemes.shikimori.presentation.view.base.fragment

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.presentation.presenter.base.BaseRateButtonListPresenter
import com.gnoemes.shikimori.presentation.view.common.adapter.BaseAdapter
import com.gnoemes.shikimori.presentation.view.rates.status.RateStatusDialog
import com.gnoemes.shikimori.utils.colorAttr
import com.gnoemes.shikimori.utils.dp
import com.gnoemes.shikimori.utils.hideRefresh
import com.gnoemes.shikimori.utils.showRefresh
import com.gnoemes.shikimori.utils.visibleIf
import com.gnoemes.shikimori.utils.widgets.VerticalSpaceItemDecorator
import kotlinx.android.synthetic.main.layout_default_list.*

/**
 * The screen side of [BaseRateButtonListPresenter]: the list, its refresh, and the status dialog.
 * The layout must include `layout_default_list`.
 */
abstract class BaseRateButtonListFragment<P : BaseRateButtonListPresenter<V, *>, V : RateButtonListView>
    : BaseFragment<P, V>(), RateButtonListView, RateStatusDialog.RateStatusCallback {

    protected abstract val listAdapter: BaseAdapter<Any>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(recyclerView) {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(VerticalSpaceItemDecorator(context.dp(8)))
        }

        refreshLayout.background = ColorDrawable(context!!.colorAttr(R.attr.colorSurface))
        refreshLayout.setOnRefreshListener { getPresenter().onRefresh() }
    }

    override fun onStatusChanged(id: Long, newStatus: RateStatus) {
        getPresenter().onChangeRateStatus(id, newStatus)
    }

    override fun showData(items: List<Any>) {
        listAdapter.bindItems(items)
    }

    override fun showStatusDialog(id: Long, title: String, status: RateStatus?, anime: Boolean) {
        hideSoftInput()
        val dialog = RateStatusDialog.newInstance(id, title, status, anime)
        dialog.show(childFragmentManager, "StatusDialog")
    }

    override fun showContent(show: Boolean) = recyclerView.visibleIf { show }
    override fun onShowLoading() = refreshLayout.showRefresh()
    override fun onHideLoading() = refreshLayout.hideRefresh()
    override fun onShowLightLoading() = refreshLayout.showRefresh()
    override fun onHideLightLoading() = refreshLayout.hideRefresh()
}

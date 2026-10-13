package com.gnoemes.shikimori.presentation.presenter.base

import android.util.Log
import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.RateButtonListItem
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.presentation.view.base.fragment.RateButtonListView
import com.gnoemes.shikimori.utils.appendLightLoadingLogic
import com.gnoemes.shikimori.utils.appendLoadingLogic
import com.gnoemes.shikimori.utils.clearAndAddAll
import io.reactivex.Single

/**
 * A list of titles, each with a status button: «Похожее» and «Хронология». The status dialog and
 * the add/change flow live here once; a subclass loads its items and shows them.
 *
 * The cards hide the button for a guest (`isGuest`), so there is no sign-in check here.
 */
abstract class BaseRateButtonListPresenter<View : RateButtonListView, Item : RateButtonListItem>(
        private val ratesInteractor: RatesInteractor,
        private val userInteractor: UserInteractor
) : BaseNetworkPresenter<View>() {

    companion object {
        private const val TAG = "RateButtonList"
    }

    protected val items = mutableListOf<Item>()

    /** null for a guest */
    protected var userId: Long? = null

    private var selectedItem: Item? = null

    /** The screen's items, converted. */
    protected abstract fun loadItems(): Single<List<Item>>

    /** Shows [loaded], already stored in [items]; [isFirst] when nothing was shown before. */
    protected abstract fun showItems(loaded: List<Item>, isFirst: Boolean)

    override fun initData() {
        loadData()
        loadUser()
    }

    private fun loadUser() =
            userInteractor.getMyUserId()
                    .subscribe({ userId = it }, { Log.w(TAG, "user not loaded", it) })
                    .addToDisposables()

    protected fun loadData() {
        loadItems()
                .appendLoadingLogic(viewState)
                .subscribe(this::setData, this::processErrors)
                .addToDisposables()
    }

    //after a status change: the list stays on screen while it reloads
    private fun reloadData() {
        loadItems()
                .appendLightLoadingLogic(viewState)
                .subscribe(this::setData, this::processErrors)
                .addToDisposables()
    }

    private fun setData(loaded: List<Item>) {
        val isFirst = items.isEmpty()
        items.clearAndAddAll(loaded)
        showItems(loaded, isFirst)
    }

    fun onShowStatusDialog(id: Long) {
        val item = items.find { it.id == id } ?: return
        selectedItem = item
        viewState.showStatusDialog(item.rateId, item.title, item.status, item.type == Type.ANIME)
    }

    /** [id] is the rate's, `Constants.NO_ID` for a title not in the list yet. */
    fun onChangeRateStatus(id: Long, newStatus: RateStatus) {
        val item = selectedItem ?: return

        (if (id == Constants.NO_ID) ratesInteractor.createRateWithResult(item.id, item.type, newStatus).ignoreElement()
        else ratesInteractor.changeRateStatus(id, newStatus))
                .subscribe(this::reloadData, this::processErrors)
                .addToDisposables()
    }

    fun onRefresh() {
        loadData()
    }
}

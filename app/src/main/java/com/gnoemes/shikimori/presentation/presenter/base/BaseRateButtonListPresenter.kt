package com.gnoemes.shikimori.presentation.presenter.base

import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.RateButtonListItem
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.user.domain.UserStatus
import com.gnoemes.shikimori.presentation.view.base.fragment.RateButtonListView
import com.gnoemes.shikimori.utils.clearAndAddAll
import io.reactivex.Single

/**
 * A list of titles, each with a status button: «Похожее» and «Хронология». The status dialog and
 * the add/change flow live here once; a subclass loads its items and shows them.
 *
 * The cards hide the button for a guest (`isGuest`). The sign-in is read on each load and each tap,
 * never kept: the app may sign the user out while the list is open (BUGS #70).
 */
abstract class BaseRateButtonListPresenter<View : RateButtonListView, Item : RateButtonListItem>(
        private val ratesInteractor: RatesInteractor,
        private val userInteractor: UserInteractor
) : BaseNetworkPresenter<View>() {

    protected val items = mutableListOf<Item>()

    //whether the shown cards were drawn for a guest; null before the first load
    private var drawnForGuest: Boolean? = null

    private var selectedItem: Item? = null

    private val isGuest: Boolean
        get() = userInteractor.getUserStatus() == UserStatus.GUEST

    /** The screen's items, converted; [isGuest] hides their status buttons. */
    protected abstract fun loadItems(isGuest: Boolean): Single<List<Item>>

    /** Shows [loaded], already stored in [items]; [isFirst] when nothing was shown before. */
    protected abstract fun showItems(loaded: List<Item>, isFirst: Boolean)

    override fun initData() {
        loadData()
    }

    //back from a title page, say, where the user signed in or was signed out
    override fun onViewReattached() {
        if (drawnForGuest != null && drawnForGuest != isGuest) reloadData()
    }

    protected fun loadData() {
        val guest = isGuest
        loadItems(guest)
                .appendLoadingLogic(viewState)
                .subscribe({ setData(it, guest) }, this::processErrors)
                .addToDisposables()
    }

    //after a status change: the list stays on screen while it reloads
    private fun reloadData() {
        val guest = isGuest
        loadItems(guest)
                .appendLightLoadingLogic(viewState)
                .subscribe({ setData(it, guest) }, this::processErrors)
                .addToDisposables()
    }

    private fun setData(loaded: List<Item>, forGuest: Boolean) {
        val isFirst = items.isEmpty()
        drawnForGuest = forGuest
        items.clearAndAddAll(loaded)
        showItems(loaded, isFirst)
    }

    fun onShowStatusDialog(id: Long) {
        val item = items.find { it.id == id } ?: return
        whenSignedIn {
            selectedItem = item
            viewState.showStatusDialog(item.rateId, item.title, item.status, item.type == Type.ANIME)
        }
    }

    /** [id] is the rate's, `Constants.NO_ID` for a title not in the list yet. */
    fun onChangeRateStatus(id: Long, newStatus: RateStatus) {
        val item = selectedItem ?: return

        whenSignedIn {
            (if (id == Constants.NO_ID) ratesInteractor.createRateWithResult(item.id, item.type, newStatus).ignoreElement()
            else ratesInteractor.changeRateStatus(id, newStatus))
                    .subscribe(this::reloadData, this::processErrors)
                    .addToDisposables()
        }
    }

    //cards drawn before an automatic sign-out still show the button: tell the user, and draw them again
    private inline fun whenSignedIn(action: () -> Unit) {
        if (!isGuest) action()
        else {
            viewState.showErrorMessage(commonResources.needAuth)
            reloadData()
        }
    }

    fun onRefresh() {
        loadData()
    }
}

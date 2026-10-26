package com.gnoemes.shikimori.presentation.presenter.rates

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.view.common.fragment.EditRateView
import javax.inject.Inject

/**
 * The list entry's sheet reads the entry itself, as shikimori has it now: the screen that opens it
 * can hold an older copy (BUGS #82–#84), and the sheet opens at once, with placeholders until the
 * entry is in. After Android closes the app, the restored sheet reads it again.
 */
@InjectViewState
class EditRatePresenter @Inject constructor(
        private val ratesInteractor: RatesInteractor
) : BaseNetworkPresenter<EditRateView>() {

    var targetId: Long = 0
    lateinit var type: Type

    override fun initData() {
        ratesInteractor.getMyRate(targetId, type)
                .subscribe({ viewState.showRate(it) }, this::onLoadError, { viewState.showRate(null) })
                .addToDisposables()
    }

    //nothing can be edited without the entry
    private fun onLoadError(throwable: Throwable) {
        showErrorText(throwable)
        viewState.close()
    }
}

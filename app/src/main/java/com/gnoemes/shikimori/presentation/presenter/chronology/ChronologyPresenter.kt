package com.gnoemes.shikimori.presentation.presenter.chronology

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.domain.chronology.ChronologyInteractor
import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.chronology.ChronologyNavigationData
import com.gnoemes.shikimori.entity.chronology.ChronologyType
import com.gnoemes.shikimori.entity.chronology.ChronologyViewModel
import com.gnoemes.shikimori.entity.common.domain.Screens
import com.gnoemes.shikimori.presentation.presenter.base.BaseRateButtonListPresenter
import com.gnoemes.shikimori.presentation.presenter.chronology.converter.ChronologyViewModelConverter
import com.gnoemes.shikimori.presentation.view.chronology.ChronologyView
import io.reactivex.Single
import javax.inject.Inject

@InjectViewState
class ChronologyPresenter @Inject constructor(
        private val interactor: ChronologyInteractor,
        private val converter: ChronologyViewModelConverter,
        private val settings: SettingsSource,
        ratesInteractor: RatesInteractor,
        userInteractor: UserInteractor
) : BaseRateButtonListPresenter<ChronologyView, ChronologyViewModel>(ratesInteractor, userInteractor) {

    lateinit var data: ChronologyNavigationData

    private var chronologyType = ChronologyType.LINKED_DIRECTLY

    override fun initData() {
        chronologyType = settings.chronologyType
        super.initData()
    }

    override fun loadItems(isGuest: Boolean): Single<List<ChronologyViewModel>> =
            interactor.getItems(data.id, data.type, chronologyType)
                    .map { converter.apply(it, isGuest) }

    //the title itself on the first load; the top on a reload
    override fun showItems(loaded: List<ChronologyViewModel>, isFirst: Boolean) {
        val scrollPos = if (isFirst) loaded.indexOfFirst { item -> item.id == data.id } else 0

        if (loaded.isEmpty()) {
            viewState.showEmptyView()
            viewState.showContent(false)
        }
        else {
            viewState.showContent(true)
            viewState.showData(loaded)
            viewState.scrollTo(scrollPos)
        }
    }

    fun onFabClicked() {
        viewState.showTypeDialog(chronologyType)
    }

    fun onTypeChanged(newType: ChronologyType) {
        chronologyType = newType
        settings.chronologyType = newType
        loadData()
    }

    fun onWebClicked() {
        onOpenWeb(franchiseUrl)
    }

    fun onShareClicked() {
        router.navigateTo(Screens.SHARE, franchiseUrl)
    }

    //the site's franchise page, under the title's own address: one built from the type and id was 404
    //for ranobe («ranobes») and for the titles whose address has a "z" before the id
    private val franchiseUrl: String
        get() = "${data.titleUrl}/franchise"
}

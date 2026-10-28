package com.gnoemes.shikimori.presentation.presenter.similar

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.domain.similar.SimilarInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.common.domain.CommonNavigationData
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.similar.presentation.SimilarViewModel
import com.gnoemes.shikimori.presentation.presenter.base.BaseRateButtonListPresenter
import com.gnoemes.shikimori.presentation.presenter.similar.converter.SimilarViewModelConverter
import com.gnoemes.shikimori.presentation.view.similar.SimilarView
import io.reactivex.Single
import javax.inject.Inject

@InjectViewState
class SimilarPresenter @Inject constructor(
        private val interactor: SimilarInteractor,
        private val converter: SimilarViewModelConverter,
        ratesInteractor: RatesInteractor,
        userInteractor: UserInteractor
) : BaseRateButtonListPresenter<SimilarView, SimilarViewModel>(ratesInteractor, userInteractor) {

    lateinit var navigationData: CommonNavigationData

    override fun loadItems(isGuest: Boolean): Single<List<SimilarViewModel>> =
            if (navigationData.type == Type.ANIME) interactor.getAnimes(navigationData.id).map { converter.apply(it, isGuest) }
            else interactor.getMangas(navigationData.id, navigationData.type).map { converter.apply(it, isGuest) }

    override fun showItems(loaded: List<SimilarViewModel>, isFirst: Boolean) {
        if (loaded.isEmpty()) viewState.showEmptyView()
        else viewState.showData(loaded)
    }
}

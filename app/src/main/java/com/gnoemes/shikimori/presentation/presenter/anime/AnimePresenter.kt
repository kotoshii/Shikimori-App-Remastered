package com.gnoemes.shikimori.presentation.presenter.anime

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.domain.anime.AnimeInteractor
import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.domain.related.RelatedInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.anime.domain.AnimeDetails
import com.gnoemes.shikimori.entity.anime.domain.Screenshot
import com.gnoemes.shikimori.entity.anime.domain.ScreenshotsNavigationData
import com.gnoemes.shikimori.entity.common.domain.*
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentType
import com.gnoemes.shikimori.entity.series.presentation.SeriesNavigationData
import com.gnoemes.shikimori.presentation.presenter.anime.converter.AnimeDetailsViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.common.converter.DetailsContentViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.presenter.rates.provider.RateResourceProvider
import com.gnoemes.shikimori.presentation.presenter.details.BaseDetailsPresenter
import com.gnoemes.shikimori.presentation.view.anime.AnimeView
import com.gnoemes.shikimori.presentation.presenter.base.appendLightLoadingLogic
import com.gnoemes.shikimori.utils.clearAndAddAll
import io.reactivex.Single
import javax.inject.Inject

@InjectViewState
open class AnimePresenter @Inject constructor(
        private val animeInteractor: AnimeInteractor,
        private val relatedInteractor: RelatedInteractor,
        override val detailsConverter: AnimeDetailsViewModelConverter,
        settingsSource: SettingsSource,
        ratesInteractor: RatesInteractor,
        userInteractor: UserInteractor,
        resourceProvider: CommonResourceProvider,
        contentConverter: DetailsContentViewModelConverter,
        rateResourceProvider: RateResourceProvider
) : BaseDetailsPresenter<AnimeView, AnimeDetails>(ratesInteractor, userInteractor, resourceProvider, contentConverter, settingsSource, rateResourceProvider) {

    private val screenshots = mutableListOf<Screenshot>()

    override val type: Type
        get() = Type.ANIME

    override fun fetchDetails(): Single<AnimeDetails> = animeInteractor.getDetails(id)

    override val characterFactory: (id: Long) -> Single<Roles>
        get() = { animeInteractor.getRoles(it) }

    override val relatedFactory: (id: Long) -> Single<List<Related>>
        get() = { relatedInteractor.getAnime(it) }

    override val linkFactory: (id: Long) -> Single<List<Link>>
        get() = { animeInteractor.getLinks(it) }

    override fun onDetailsLoaded(details: AnimeDetails) {
        super.onDetailsLoaded(details)
        viewState.setContentItem(DetailsContentType.VIDEO, contentConverter.apply(details.videos ?: emptyList()))
        loadScreenshots()
    }

    private fun loadScreenshots() =
            animeInteractor.getScreenshots(id)
                    .doOnSuccess { screenshots.clearAndAddAll(it) }
                    .appendLightLoadingLogic(viewState)
                    .map(contentConverter)
                    .subscribe({ viewState.setContentItem(DetailsContentType.SCREENSHOTS, it) }, this::processErrors)
                    .addToDisposables()

    override fun onWatchOnline() {
        val anime = details ?: return

        val data = SeriesNavigationData.of(id, anime.image, title, anime.status, anime.episodes, anime.episodesAired,
                anime.userRate?.id, anime.userRate?.episodes ?: 0)
        router.navigateTo(Screens.SERIES, data)
    }

    override fun onScreenshotsClicked(pos: Int) {
        if (details == null) return

        //the name the series screen gets too, so an anime's episodes and screenshots are saved
        //under the same folder name
        val data = ScreenshotsNavigationData(pos, screenshots, title)
        router.navigateTo(Screens.SCREENSHOTS, data)
    }
}

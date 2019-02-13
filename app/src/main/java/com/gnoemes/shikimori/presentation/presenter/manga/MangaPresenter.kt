package com.gnoemes.shikimori.presentation.presenter.manga

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.manga.MangaInteractor
import com.gnoemes.shikimori.domain.ranobe.RanobeInteractor
import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.domain.related.RelatedInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.common.domain.*
import com.gnoemes.shikimori.entity.common.presentation.DetailsHeadItem
import com.gnoemes.shikimori.entity.manga.domain.MangaDetails
import com.gnoemes.shikimori.entity.roles.domain.Character
import com.gnoemes.shikimori.presentation.presenter.common.converter.DetailsContentViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.common.converter.FranchiseNodeViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.common.converter.LinkViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.presenter.details.BaseDetailsPresenter
import com.gnoemes.shikimori.presentation.presenter.manga.converter.MangaDetailsViewModelConverter
import com.gnoemes.shikimori.presentation.view.manga.MangaView
import com.gnoemes.shikimori.utils.appendLoadingLogic
import io.reactivex.Single
import javax.inject.Inject

@InjectViewState
class MangaPresenter @Inject constructor(
        private val mangaInteractor: MangaInteractor,
        private val ranobeInteractor: RanobeInteractor,
        private val relatedInteractor: RelatedInteractor,
        private val detailsConverter: MangaDetailsViewModelConverter,
        ratesInteractor: RatesInteractor,
        userInteractor: UserInteractor,
        resourceProvider: CommonResourceProvider,
        linkConverter: LinkViewModelConverter,
        nodeConverter: FranchiseNodeViewModelConverter,
        contentConverter: DetailsContentViewModelConverter
) : BaseDetailsPresenter<MangaView>(ratesInteractor, userInteractor, resourceProvider, linkConverter, nodeConverter, contentConverter) {

    var isRanobe: Boolean = false

    private lateinit var currentManga: MangaDetails

    override val type: Type
        get() = if (isRanobe) Type.RANOBE else Type.MANGA

    override fun loadContent(showLoading: Boolean): Single<DetailsHeadItem> =
            loadDetails()
                    .flatMap {
                        if (showLoading) Single.just(it).appendLoadingLogic(viewState)
                        else Single.just(it)
                    }
                    .doOnSuccess { loadDescription() }
                    .doOnSuccess { loadOptions() }

    override fun loadDetails(): Single<DetailsHeadItem> =
            (if (isRanobe) ranobeInteractor.getDetails(id)
            else mangaInteractor.getDetails(id))
                    .doOnSuccess { currentManga = it; rateId = it.userRate?.id ?: Constants.NO_ID }
                    .map { detailsConverter.convertHead(it) }

    override val characterFactory: (id: Long) -> Single<List<Character>>
        get() = {
            (if (isRanobe) ranobeInteractor.getRoles(it)
            else mangaInteractor.getRoles(it))
        }

    override val similarFactory: (id: Long) -> Single<List<LinkedContent>>
        get() = {
            (if (isRanobe) ranobeInteractor.getSimilar(it)
            else mangaInteractor.getSimilar(it))
                    .map { it.map { it as LinkedContent } }
        }

    override val relatedFactory: (id: Long) -> Single<List<Related>>
        get() = {
            (if (isRanobe) relatedInteractor.getRanobe(it)
            else relatedInteractor.getManga(it))
        }

    override val linkFactory: (id: Long) -> Single<List<Link>>
        get() = {
            (if (isRanobe) ranobeInteractor.getLinks(it)
            else mangaInteractor.getLinks(it))
        }

    override val chronologyFactory: (id: Long) -> Single<List<FranchiseNode>>
        get() = {
            (if (isRanobe) ranobeInteractor.getFranchiseNodes(it)
            else mangaInteractor.getFranchiseNodes(it))
        }


    private fun loadDescription() {
        val descriptionItem = detailsConverter.convertDescriptionItem(currentManga.description)
        viewState.setDescriptionItem(descriptionItem)
    }

    override fun loadOptions() {
        val optionsItem = detailsConverter.convertOptions(currentManga, userId == Constants.NO_ID)
        viewState.setOptionsItem(optionsItem)
    }

    override fun onOpenDiscussion() {
        currentManga.topicId?.let { onTopicClicked(it) }
                ?: router.showSystemMessage(resourceProvider.topicNotFound)
    }

    override fun onEditRate() {
        viewState.showRateDialog(currentManga.userRate)
    }

    override fun onOpenInBrowser() = onOpenWeb(currentManga.url)

    override fun onWatchOnline() {
        //TODO chapters screen
    }
}
package com.gnoemes.shikimori.presentation.presenter.manga

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.domain.manga.MangaInteractor
import com.gnoemes.shikimori.domain.rates.RatesInteractor
import com.gnoemes.shikimori.domain.related.RelatedInteractor
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.common.domain.*
import com.gnoemes.shikimori.entity.manga.domain.MangaDetails
import com.gnoemes.shikimori.presentation.presenter.common.converter.DetailsContentViewModelConverter
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.presenter.details.BaseDetailsPresenter
import com.gnoemes.shikimori.presentation.presenter.manga.converter.MangaDetailsViewModelConverter
import com.gnoemes.shikimori.presentation.view.manga.MangaView
import io.reactivex.Single
import javax.inject.Inject

@InjectViewState
class MangaPresenter @Inject constructor(
        private val mangaInteractor: MangaInteractor,
        private val relatedInteractor: RelatedInteractor,
        override val detailsConverter: MangaDetailsViewModelConverter,
        settingsSource: SettingsSource,
        ratesInteractor: RatesInteractor,
        userInteractor: UserInteractor,
        resourceProvider: CommonResourceProvider,
        contentConverter: DetailsContentViewModelConverter
) : BaseDetailsPresenter<MangaView, MangaDetails>(ratesInteractor, userInteractor, resourceProvider, contentConverter, settingsSource) {

    //MANGA or RANOBE: first what the screen was opened with, then what the loaded title says
    override var type: Type = Type.MANGA

    //the title decides, not the screen that opened the page - «Мой список» opens every entry of the
    //manga tab as a manga. /api/mangas answers for a ranobe too (checked 2026-10-14), so a page
    //opened as a manga still loads, and from here on its genre tap, similar and chronology are
    //the ranobe's
    override fun fetchDetails(): Single<MangaDetails> = mangaInteractor.getDetails(id, type)
            .doOnSuccess { type = if (it.type.isRanobe) Type.RANOBE else Type.MANGA }

    override val characterFactory: (id: Long) -> Single<Roles>
        get() = { mangaInteractor.getRoles(it, type) }

    override val relatedFactory: (id: Long) -> Single<List<Related>>
        get() = { relatedInteractor.getManga(it, type) }

    override val linkFactory: (id: Long) -> Single<List<Link>>
        get() = { mangaInteractor.getLinks(it, type) }
}

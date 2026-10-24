package com.gnoemes.shikimori.presentation.view.series

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.series.domain.PlayerType
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import com.gnoemes.shikimori.entity.series.presentation.EmbeddedPlayerNavigationData
import com.gnoemes.shikimori.entity.series.presentation.EpisodeControls
import com.gnoemes.shikimori.entity.series.presentation.EpisodesNavigationData
import com.gnoemes.shikimori.entity.series.presentation.SeriesDownloadItem
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.entity.series.presentation.TranslationViewModel
import com.gnoemes.shikimori.presentation.presenter.common.AddToEndSingleTagStrategy
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragmentView

interface SeriesView : BaseFragmentView {

    fun showData(newItems: List<TranslationViewModel>)

    /**
     * The toolbar's own controls: the source button, the type button showing [type], and ‹ «# N» ›. A
     * null [type] or [episodes] hides its buttons.
     */
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showControls(isSourceShown: Boolean, type: TranslationType?, episodes: EpisodeControls?)

    fun changeSource(isAlternative : Boolean)

    @StateStrategyType(AddToEndSingleTagStrategy::class, tag = "search")
    fun showSearchView()

    @StateStrategyType(AddToEndSingleTagStrategy::class, tag = "search")
    fun onSearchClosed()

    @StateStrategyType(SkipStrategy::class)
    fun showPlayerDialog(playing: EmbeddedPlayerNavigationData)

    @StateStrategyType(SkipStrategy::class)
    fun showDownloadHostingDialog(title : String, videos: List<TranslationVideo>, hasUnsupported: Boolean)

    @StateStrategyType(SkipStrategy::class)
    fun showDownloadDialog(title : String, items: List<SeriesDownloadItem>)

    @StateStrategyType(SkipStrategy::class)
    fun showAuthorDialog(author: String)

    @StateStrategyType(SkipStrategy::class)
    fun showEpisodesDialog(data: EpisodesNavigationData)

    @StateStrategyType(SkipStrategy::class)
    fun checkPermissions()

    @StateStrategyType(SkipStrategy::class)
    fun showFolderChooserDialog()

    @StateStrategyType(SkipStrategy::class)
    fun showQualityChooser(video: TranslationVideo, playerType: PlayerType, tracks: List<Track>)

    /** Opens [url] in another app, and tells the presenter when one opened: [video]'s episode counts then. */
    @StateStrategyType(OneExecutionStateStrategy::class)
    fun openExternalPlayer(video: TranslationVideo, url: String?)

    fun showFab(show : Boolean)

    fun setBackground(image: Image)

    fun showEmptyAuthorsView(show: Boolean, isAlternative: Boolean = false)

    /** The source does not list the episode; hidden with [showEmptyAuthorsView] (false), which shares its place. */
    fun showEpisodeNotFoundView(isAlternative: Boolean)

    fun showEmptySearchView()

    @StateStrategyType(SkipStrategy::class)
    fun showTracksNotFoundError()

    @StateStrategyType(SkipStrategy::class)
    fun showAnime365LoginRequired()

    @StateStrategyType(SkipStrategy::class)
    fun showHostingChallengeError()

    @StateStrategyType(SkipStrategy::class)
    fun scrollToPosition(position: Int)
}
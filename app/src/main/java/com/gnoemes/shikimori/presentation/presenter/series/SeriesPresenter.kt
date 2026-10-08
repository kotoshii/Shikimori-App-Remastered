package com.gnoemes.shikimori.presentation.presenter.series

import android.util.Log
import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.repository.series.shikimori.parser.HostingParsers
import com.gnoemes.shikimori.data.repository.series.smotretanime.Anime365TokenSource
import com.gnoemes.shikimori.domain.download.DownloadInteractor
import com.gnoemes.shikimori.domain.series.SeriesInteractor
import com.gnoemes.shikimori.domain.series.WatchProgressInteractor
import com.gnoemes.shikimori.entity.app.domain.exceptions.HostingChallengeException
import com.gnoemes.shikimori.entity.common.domain.Screens
import com.gnoemes.shikimori.entity.download.DownloadVideoData
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.*
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.presenter.common.provider.ShareResourceProvider
import com.gnoemes.shikimori.presentation.presenter.series.translations.converter.TranslationsViewModelConverter
import com.gnoemes.shikimori.presentation.view.series.SeriesView
import com.gnoemes.shikimori.utils.appendLoadingLogic
import com.gnoemes.shikimori.utils.clearAndAddAll
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

@InjectViewState
class SeriesPresenter @Inject constructor(
        private val interactor: SeriesInteractor,
        private val downloadInteractor: DownloadInteractor,
        private val settingsSource: SettingsSource,
        private val converter: TranslationsViewModelConverter,
        private val commonResourceProvider: CommonResourceProvider,
        private val shareResourceProvider: ShareResourceProvider,
        private val tokenSource: Anime365TokenSource,
        private val parsers: HostingParsers,
        private val progressInteractor: WatchProgressInteractor
) : BaseNetworkPresenter<SeriesView>() {

    lateinit var navigationData: SeriesNavigationData
    lateinit var type: TranslationType

    private var episode: Int? = null
    private var episodeId: Long? = null
    private var isAlternative: Boolean = settingsSource.altSourceByDefault
    private var setting: TranslationSetting? = null
    private var query: String? = null

    private val items = mutableListOf<TranslationViewModel>()
    private lateinit var selectedVideo: TranslationVideo
    private var selectedDownloadUrl: String? = null
    private var selectedDownloadAudioUrl: String? = null
    private var selectedDownloadVideo: Video? = null
    private var selectedPlayer: PlayerType? = null

    private var isWatchSession = false

    override fun initData() {
        super.initData()
        type = settingsSource.translationType
        episode = if (navigationData.episodesAired < navigationData.episode ?: 0) navigationData.episodesAired else navigationData.episode
        episodeId = episode?.toLong()
        //the episode list and the player read the count from here, and add what they write to it
        progressInteractor.remember(navigationData.animeId, navigationData.rateId, navigationData.watchedEpisodes)

        viewState.setBackground(navigationData.image)
        viewState.setTitle(navigationData.name)
        viewState.changeSource(isAlternative)

        if (episode != null) {
            viewState.setEpisodeName(episode!!)
            viewState.showNextEpisode(episode != navigationData.episodesAired)
        } else viewState.showFab(false)

        if (navigationData.episodesAired == 1) {
            viewState.hideEpisodeName()
            viewState.showNextEpisode(false)
        }

        loadWithEpisode()
    }

    override fun onViewReattached() {
        super.onViewReattached()

        if (isWatchSession) {
            loadWithEpisode()
        }
    }

    private fun loadWithEpisode() {
        if (episodeId != null) loadData(episodeId!!)
        else loadEpisodes()
    }

    private fun loadData(episodeId: Long) =
            loadSettingsIfNeed()
                    .flatMap { loadTranslations(it, episodeId) }
                    .appendLoadingLogic(viewState)
                    .doOnSubscribe { viewState.showEmptyAuthorsView(false) }
                    .subscribe(this::setData, this::processErrors)
                    .addToDisposables()

    private fun loadEpisodes() = interactor.getEpisodes(navigationData.animeId, isAlternative)
            .map { it.take(navigationData.episodesAired) }
            .doOnSubscribe { viewState.showEpisodeLoading(true) }
            .doOnSuccess { viewState.showEpisodeLoading(false) }
            .subscribe(this::openPriorityEpisode, this::processErrors)
            .addToDisposables()

    private fun loadTranslations(type: TranslationType, episodeId: Long) = interactor
            .getTranslations(type, navigationData.animeId, episodeId, isAlternative, true)
            .doOnSubscribe { viewState.setTranslationType(type) }
            .map { converter.convertTranslations(it, setting) }

    private fun loadSettingsIfNeed(): Single<TranslationType> = (if (setting == null && settingsSource.useLocalTranslationSettings) loadSettings() else Single.just(type))

    private fun loadSettings() =
            interactor.getTranslationSettings(navigationData.animeId)
                    .doOnSuccess { setting = it }
                    .map { type = it.lastType ?: settingsSource.translationType;type }

    fun onRefresh() = loadWithEpisode()

    private fun openPriorityEpisode(items: List<Episode>) {
        if (episode == null) episode = if (!isWatchSession) items.firstOrNull { !it.isWatched }?.index ?: items.lastOrNull { it.isWatched }?.index else items.lastOrNull { it.isWatched }?.index ?: items.firstOrNull()?.index
        episodeId = items.firstOrNull { it.index == episode }?.id
        isWatchSession = false
        if (episode != null && episodeId != null) {
            viewState.showNextEpisode(episode != navigationData.episodesAired)
            viewState.setEpisodeName(episode!!)
            loadData(episodeId!!)
            viewState.showFab(true)
        } else if (navigationData.episodesAired > 0) {
            if (episode == null) episode = 1
            viewState.setEpisodeName(episode!!)
            viewState.onHideLoading()
            viewState.hideEmptyView()
            viewState.showNextEpisode(episode != navigationData.episodesAired)
            viewState.showEmptyAuthorsView(true, isAlternative)
            viewState.showContent(false)
            viewState.showFab(true)
        } else {
            viewState.onHideLoading()
            viewState.showEmptyView()
            viewState.hideEpisodeName()
            viewState.showNextEpisode(false)
            viewState.showFab(false)
        }
    }

    private fun setData(data: List<TranslationViewModel>) {
        val items = data.toMutableList()
        if (items.find { it.isSameAuthor } != null) {
            val priorityItem = items[items.indexOfFirst { it.isSameAuthor }]
            items.remove(priorityItem)
            items.add(0, priorityItem)
        }
        this@SeriesPresenter.items.clearAndAddAll(items)
        showData(items)
    }

    private fun showData(it: List<TranslationViewModel>) {
        if (!query.isNullOrBlank() && it.isEmpty()) viewState.showEmptySearchView()
        else if (it.isEmpty()) {
            viewState.showEmptyAuthorsView(true, isAlternative)
            viewState.showContent(false)
        } else viewState.showData(it)
    }

    fun onNextEpisode() = interactor.getEpisodes(navigationData.animeId, isAlternative)
            .map { it.take(navigationData.episodesAired) }
            .doOnSubscribe { viewState.showEpisodeLoading(true) }
            .doOnSuccess { viewState.showEpisodeLoading(false) }
            .subscribe(this::loadNextEpisode, this::processErrors)
            .addToDisposables()


    private fun loadNextEpisode(items: List<Episode>) {
        episode = items.firstOrNull { it.index == episode?.plus(1) }?.index ?: episode
        episodeId = items.firstOrNull { it.index == episode }?.id
        if (episode != null && episodeId != null) {
            viewState.showNextEpisode(episode != navigationData.episodesAired)
            viewState.setEpisodeName(episode!!)
            loadData(episodeId!!)
            viewState.showFab(true)
        } else if (navigationData.episodesAired > 0) {
            if (episode == null) episode = 0
            episode = episode?.plus(1)
            viewState.setEpisodeName(episode!!)
            viewState.showNextEpisode(episode != navigationData.episodesAired)
            viewState.onHideLoading()
            viewState.hideEmptyView()
            viewState.showEmptyAuthorsView(true, isAlternative)
            viewState.showContent(false)
            viewState.showFab(true)
        } else {
            viewState.onHideLoading()
            viewState.showEmptyView()
            viewState.hideEpisodeName()
            viewState.showNextEpisode(false)
            viewState.showFab(false)
        }
    }

    fun onSearchClicked() {
        viewState.showSearchView()
    }

    fun onSearchClose() = viewState.onSearchClosed()

    fun onMenuClicked(category: TranslationMenu) = when (category) {
        is TranslationMenu.Download -> showDownloadDialog(category.videos)
        is TranslationMenu.Author -> showAuthorDialog(category.author)
    }

    fun onEpisodeSelected(episodeId: Long, episode: Int, alternative: Boolean) {
        this.episodeId = episodeId
        this.episode = episode

        viewState.setEpisodeName(episode)
        viewState.showNextEpisode(episode != navigationData.episodesAired)

        if (isAlternative != alternative) {
            viewState.changeSource(alternative)
            isAlternative = alternative
        }
        loadWithEpisode()
    }

    fun onShare(item: SeriesDownloadItem) {
        //The url is shared exactly as the player uses it. An hls link used to be cut back at ".mp4"
        //to turn it into a plain file, which kodik's urls are shaped for
        //(.../720.mp4:hls:manifest.m3u8) - but that file is gone: verified 2026-09-01, the full
        //playlist answers 200 and the shortened one 500, so sharing it handed people a dead link.
        //It was a no-op everywhere else anyway, since no other hosting puts ".mp4" in an hls url.
        val videoUrl = item.url

        //the same four things the downloaded file is named after, so a shared link and a saved file
        //describe the episode alike. Blanks drop out rather than leaving separators behind - an
        //unknown author, "не выбрано" for the kind, or a parser that could not tell the quality
        val details = listOf(
                item.video.author,
                item.video.translationType?.takeIf { it != TranslationType.ALL }?.localizedType.orEmpty(),
                item.quality.orEmpty(),
                item.hosting
        ).filter { it.isNotBlank() }

        val text = shareResourceProvider.getEpisodeShareFormattedMessage(navigationData.name, episode!!, videoUrl, details)
        router.navigateTo(Screens.SHARE, text)
    }

    private fun showAuthorDialog(author: String) {
        viewState.showAuthorDialog(author)
    }

    /**
     * Anime365 is deliberately **not** filtered out when there is no token. Dropping it silently
     * left the user with a shorter list and no reason for it - the login is asked for at the moment
     * they pick it instead, see [needsAnime365Login]. Hiding it outright is a setting of its own,
     * `hideAnime365`, which is the user's choice rather than ours.
     */
    private fun showDownloadDialog(videos: List<TranslationVideo>) {
        val filteredItems = videos.filter { parsers.isSupported(it.videoHosting) }

        if (filteredItems.isEmpty()) return

        //asking which hosting first means only that one is resolved. Resolving the whole group up
        //front cost a page fetch per hosting, and all but the chosen one were thrown away
        if (filteredItems.size == 1) showDownloadQualities(filteredItems)
        else viewState.showDownloadHostingDialog(filteredItems.first().author, filteredItems, filteredItems.size < videos.size)
    }

    fun onDownloadHostingSelected(video: TranslationVideo) = showDownloadQualities(listOf(video))

    private fun showDownloadQualities(videos: List<TranslationVideo>) {
        if (videos.all(::needsAnime365Login)) {
            viewState.showAnime365LoginRequired()
            return
        }

        //remembered rather than thrown, because a failed hosting is dropped from the list instead
        //of failing it - but if that leaves nothing at all, the reason is worth reporting
        var challenged = false

        Observable.fromIterable(videos)
                .flatMapSingle { payload ->
                    interactor.getVideo(payload)
                            .map { video -> video.tracks.map { converter.convertTrack(video, it) } }
                            //a hosting that cannot be resolved is left out rather than failing the
                            //whole list, which matters when more than one is being resolved
                            .onErrorReturn {
                                if (it is HostingChallengeException) challenged = true
                                emptyList<SeriesDownloadItem>()
                            }
                }
                .flatMapIterable { it }
                .toList()
                .appendLoadingLogic(viewState)
                .subscribe({ items ->
                    when {
                        items.isNotEmpty() -> viewState.showDownloadDialog(videos.first().author, items)
                        challenged -> viewState.showHostingChallengeError()
                        else -> viewState.showTracksNotFoundError()
                    }
                }, this::processErrors)
                .addToDisposables()
    }

    fun showEpisodes() {
        val data = EpisodesNavigationData(navigationData.animeId, episode!!, isAlternative)
        viewState.showEpisodesDialog(data)
    }

    fun onTypeChanged(newType: TranslationType) {
        if (type == newType) return
        this.type = newType
        loadWithEpisode()
    }

    fun onSourceChanged(alternative: Boolean) {
        if (isAlternative == alternative) return

        viewState.showData(emptyList())
        viewState.changeSource(alternative)
        isAlternative = alternative
        episodeId = null
        loadWithEpisode()
    }

    fun onDiscussionClicked() {
        if (episode != null) {
            interactor.getTopic(navigationData.animeId, episode!!)
                    .subscribe(this::onTopicClicked, this::onDiscussionNotExist)
                    .addToDisposables()
        }
    }

    fun onQueryChanged(newText: String?) {
        query = newText

        if (newText.isNullOrBlank()) showData(items)
        else {
            val searchItems = items.filter { it.authors.contains(newText, true) }
            showData(searchItems)
        }

        viewState.scrollToPosition(0)
    }

    fun onPlayerSelected(playerType: PlayerType) {
        openVideo(selectedVideo, playerType)
    }

    fun onHostingClicked(video: TranslationVideo) {
        this.selectedVideo = video
        if (!parsers.isSupported(video.videoHosting)) openVideo(video, PlayerType.WEB)
        else if (!settingsSource.isAskForPlayer) openVideo(video, settingsSource.playerType)
        else viewState.showPlayerDialog()
    }

    //Only embedded player can process object payload
    //Others o uses urls
    private fun openVideo(payload: TranslationVideo, playerType: PlayerType) {
        //anime365 resolves nothing without a login, so say so instead of opening a player that will
        //find no tracks. The web player is the exception - it loads anime365's own page, which asks
        //for the login itself
        if (playerType != PlayerType.WEB && needsAnime365Login(payload)) {
            viewState.showAnime365LoginRequired()
            return
        }

        if (playerType == PlayerType.EMBEDDED) openPlayer(playerType, EmbeddedPlayerNavigationData(navigationData.name, items.firstOrNull()!!.episodesSize, payload, isAlternative))
        else if (playerType == PlayerType.WEB && payload.webPlayerUrl != null) openPlayer(playerType, payload.webPlayerUrl)
        else getVideoAndExecute(payload) { selectedPlayer = playerType; showQualityChooser(it.tracks) }
    }

    /** Anime365 needs a paid account, and without the token nothing it hands back has tracks. */
    private fun needsAnime365Login(video: TranslationVideo): Boolean =
            video.videoHosting is VideoHosting.SMOTRET_ANIME && tokenSource.getToken() == null

    private fun showQualityChooser(tracks: List<Track>) {
        if (tracks.isEmpty()) {
            viewState.showTracksNotFoundError()
            return
        }
        if (tracks.size == 1 || settingsSource.isExternalBestQuality) openPlayer(selectedPlayer!!, tracks.firstOrNull()?.url)
        else viewState.showQualityChooser(tracks.map { Pair(it.quality, it.url) })
    }

    fun onQualityChoosed(url: String?) {
        openPlayer(selectedPlayer!!, url)
    }

    override fun openPlayer(playerType: PlayerType, payload: Any?) {
        super.openPlayer(playerType, payload)

        isWatchSession = true
        saveSettingsAndIncrementOptional(playerType != PlayerType.EMBEDDED, selectedVideo)
    }

    private fun saveSettingsAndIncrementOptional(increment: Boolean, payload: TranslationVideo) {
        //the built-in player counts the episode itself, once it starts playing. A failed count is
        //only logged: it happens in the background and must not cover the screen with an error
        if (increment) {
            progressInteractor.episodePlayed(payload.animeId, episode!!)
                    .subscribe({}, { Log.w(TAG, "could not count episode $episode", it) })
                    .addToDisposables()
        }

        interactor.saveTranslationSettings(TranslationSetting(payload.animeId, payload.author, payload.type))
                .subscribe({}, this::processErrors)
                .addToDisposables()
    }

    private fun getVideoAndExecute(payload: TranslationVideo, onSubscribe: (Video) -> Unit) {
        interactor.getVideo(payload)
                .appendLoadingLogic(viewState)
                .subscribe(onSubscribe::invoke, this::processVideoErrors)
                .addToDisposables()
    }

    /**
     * A hosting that asks for an anti-bot check has a cause worth naming - otherwise it reads as
     * the video being gone, which is what the generic path says.
     */
    private fun processVideoErrors(throwable: Throwable) {
        if (throwable is HostingChallengeException) viewState.showHostingChallengeError()
        else processErrors(throwable)
    }

    fun onTrackForDownloadSelected(url: String, video: Video) {
        //Kodik urls used to be truncated at ".mp4" here to turn the hls manifest into a direct file,
        //because DownloadManager could only fetch one url. That file stopped existing - the shortened
        //url answers 500 - and DownloadService walks the playlist itself now, so the url is
        //passed through untouched.
        selectedDownloadUrl = url
        selectedDownloadAudioUrl = video.tracks.find { it.url == url }?.audioUrl
        selectedDownloadVideo = video
        viewState.checkPermissions()
    }

    fun onStoragePermissionsAccepted() {
        val downloadPath = settingsSource.downloadFolder

        if (downloadPath.isNotEmpty()) downloadVideo(selectedDownloadUrl, selectedDownloadAudioUrl, selectedDownloadVideo)
        else viewState.showFolderChooserDialog()
    }

    private fun downloadVideo(url: String?, audioUrl: String?, video: Video?) {
        val data = DownloadVideoData(
                navigationData.animeId, navigationData.name, episode!!, url, audioUrl,
                parsers.headers(video),
                author = video?.author.orEmpty(),
                //the chosen track carries the quality; "unknown" is what parsers use when they
                //cannot tell, and the ui hides it the same way
                quality = video?.tracks?.find { it.url == url }?.quality
                        ?.takeIf { it.isNotBlank() && it != "unknown" }
                        ?.let { "${it}p" }
                        .orEmpty(),
                kind = video?.translationType?.takeIf { it != TranslationType.ALL }?.localizedType.orEmpty(),
                //domain-shaped for every hosting ("vk.com", "cda.pl")
                hosting = video?.hosting?.synonymType.orEmpty()
        )
        downloadInteractor.downloadVideo(data)
                .subscribe({}, this::processDownloadErrors)
                .addToDisposables()
    }

    private fun onDiscussionNotExist(throwable: Throwable?) {
        router.showSystemMessage(commonResourceProvider.topicNotFound)
        viewState.showFab(false)
    }

    private fun processDownloadErrors(throwable: Throwable) {
    }

    companion object {
        private const val TAG = "WatchProgress"
    }
}
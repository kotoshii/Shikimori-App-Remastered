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
import com.gnoemes.shikimori.presentation.presenter.base.appendLoadingLogic
import com.gnoemes.shikimori.utils.clearAndAddAll
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

    //the type this title was last watched in is read before its first translations; until then the
    //type button stays hidden, so a type picked by hand is never replaced by the saved one
    private var isTypeKnown = false
    //the episode list is loading, so which episode will show is not known yet
    private var isLoadingEpisodes = false
    //no episode is out at all: neither the source nor the type can change that
    private var isNothingOut = false

    private val items = mutableListOf<TranslationViewModel>()
    private lateinit var selectedVideo: TranslationVideo
    private var selectedDownload: SeriesDownloadItem? = null
    private var selectedPlayer: PlayerType? = null

    private var isWatchSession = false

    //where the list on screen comes from. A source, type or episode switch, «Повторить» and the
    //return from a player each replace the load still running, so its late answer can no longer
    //cover the new one (BUGS #52)
    private val listLoad = loadSlot()
    //the hosting's page, looked up for «Внешний плеер» or «Скачать». A list load cancels it too; it
    //never cancels a list load
    private val videoLookup = loadSlot()

    override fun initData() {
        super.initData()
        type = settingsSource.translationType
        isTypeKnown = !settingsSource.useLocalTranslationSettings
        episode = if (navigationData.episodesAired < navigationData.episode ?: 0) navigationData.episodesAired else navigationData.episode
        episodeId = episode?.toLong()
        //the episode list and the player read the count from here, and add what they write to it
        progressInteractor.remember(navigationData.animeId, navigationData.rateId, navigationData.watchedEpisodes)

        viewState.setBackground(navigationData.image)
        viewState.setTitle(navigationData.name)
        viewState.changeSource(isAlternative)
        if (episode == null) viewState.showFab(false)

        loadWithEpisode()
    }

    //back from a player the list loads again - once: every later return to the screen used to reload
    //it too (BUGS #62)
    override fun onViewReattached() {
        super.onViewReattached()

        if (isWatchSession) {
            loadWithEpisode()
            isWatchSession = false
        }
    }

    private fun loadWithEpisode() {
        val id = episodeId
        if (id != null) load(translations(id))
        //with no episode asked for: the first one not watched, else the last watched
        else loadEpisodes { items ->
            if (episode == null) episode = items.firstOrNull { !it.isWatched }?.index ?: items.lastOrNull { it.isWatched }?.index
            episode ?: 1
        }
    }

    //the next episode when the source has it, else the current one again. When the source has
    //neither, the next one, as having no translations yet
    fun onNextEpisode() = loadEpisodes { items ->
        episode = items.firstOrNull { it.index == episode?.plus(1) }?.index ?: episode
        (episode ?: 0) + 1
    }

    /**
     * The episode list, then the episode [pick] sets: [pick] returns the one shown as having no
     * translations yet when the source lacks it (see [showEpisode]).
     */
    private fun loadEpisodes(pick: (List<Episode>) -> Int) = load(
            loadSettingsIfNeed()
                    .flatMap { interactor.getEpisodes(navigationData.animeId, isAlternative) }
                    .map { it.take(navigationData.episodesAired) }
                    .flatMap { showEpisode(it, pick(it)) },
            loadsEpisodes = true)

    private fun translations(episodeId: Long): Single<Loaded> =
            loadSettingsIfNeed()
                    .flatMap { interactor.getTranslations(it, navigationData.animeId, episodeId, isAlternative, true) }
                    .map { Loaded.Translations(converter.convertTranslations(it, setting)) }

    //the type this title was last watched in, read once, when that setting is on
    private fun loadSettingsIfNeed(): Single<TranslationType> =
            if (isTypeKnown) Single.just(type)
            else interactor.getTranslationSettings(navigationData.animeId)
                    .doOnSuccess {
                        setting = it
                        type = it.lastType ?: settingsSource.translationType
                        isTypeKnown = true
                        showControls()
                    }
                    .map { type }

    /** Replaces whatever the list was loading, and a video lookup, with [single]. */
    private fun load(single: Single<Loaded>, loadsEpisodes: Boolean = false) {
        videoLookup.cancel()
        listLoad.start {
            isLoadingEpisodes = loadsEpisodes
            showControls()
            single.appendLoadingLogic(viewState)
                    .doOnSubscribe { viewState.showEmptyAuthorsView(false) }
                    .subscribe(this::onLoaded, this::onLoadFailed)
        }
    }

    fun onRefresh() = loadWithEpisode()

    /**
     * The chosen [episode]: its translations when the source has it. If not, while anything is out,
     * episode [notFound] as having no translations yet; else that nothing is out.
     */
    private fun showEpisode(items: List<Episode>, notFound: Int): Single<Loaded> {
        val id = items.firstOrNull { it.index == episode }?.id
        episodeId = id
        isLoadingEpisodes = false
        isNothingOut = id == null && navigationData.episodesAired <= 0
        if (id == null && !isNothingOut) episode = notFound
        showControls()
        viewState.showFab(!isNothingOut)

        return when {
            id != null -> translations(id)
            isNothingOut -> Single.just(Loaded.NothingOut)
            else -> Single.just(Loaded.NoTranslations)
        }
    }

    private fun onLoaded(loaded: Loaded) {
        when (loaded) {
            is Loaded.Translations -> setData(loaded.items)
            Loaded.NoTranslations -> {
                viewState.hideEmptyView()
                viewState.showEmptyAuthorsView(true, isAlternative)
                viewState.showContent(false)
            }
            Loaded.NothingOut -> viewState.showEmptyView()
        }
    }

    //whatever failed, the source can still be switched and «Повторить» tries again (BUGS #53)
    private fun onLoadFailed(throwable: Throwable) {
        isLoadingEpisodes = false
        showControls()
        processErrors(throwable)
    }

    /**
     * Which toolbar controls show (BUGS #53): the source and the type whenever they can change
     * anything; the episode chip and › only for a known episode of a title with more than one, and
     * not while the episode list loads - a tap there needs the episode.
     */
    private fun showControls() {
        val shownEpisode = episode.takeIf { !isLoadingEpisodes && !isNothingOut && navigationData.episodesAired != 1 }
        viewState.showControls(
                !isNothingOut,
                type.takeIf { isTypeKnown && !isNothingOut },
                shownEpisode,
                shownEpisode != null && shownEpisode != navigationData.episodesAired)
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
        val videoUrl = item.track.url

        //the same four things the downloaded file is named after, so a shared link and a saved file
        //describe the episode alike. Blanks drop out rather than leaving separators behind
        val details = listOf(item.author, item.kind, item.quality, item.hosting).filter { it.isNotBlank() }

        //the video's own episode, not the screen's (BUGS #69 B8)
        val text = shareResourceProvider.getEpisodeShareFormattedMessage(navigationData.name, item.video.episodeId.toInt(), videoUrl, details)
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
        if (filteredItems.size == 1) showDownloadQualities(filteredItems.first())
        else viewState.showDownloadHostingDialog(filteredItems.first().author, filteredItems, filteredItems.size < videos.size)
    }

    fun onDownloadHostingSelected(video: TranslationVideo) = showDownloadQualities(video)

    //looked up as for a player, and a failure reads the same: a connection that failed is no longer
    //taken for a deleted video (BUGS #61)
    private fun showDownloadQualities(video: TranslationVideo) {
        if (needsAnime365Login(video)) {
            viewState.showAnime365LoginRequired()
            return
        }

        getVideoAndExecute(video) { found ->
            val items = found.tracks.map { converter.convertTrack(found, it) }
            if (items.isEmpty()) viewState.showTracksNotFoundError()
            else viewState.showDownloadDialog(video.author, items)
        }
    }

    fun showEpisodes() {
        val episode = episode ?: return
        viewState.showEpisodesDialog(EpisodesNavigationData(navigationData.animeId, episode, isAlternative))
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
        //only logged: it happens in the background and must not cover the screen with an error.
        //The video's own episode, not the screen's (BUGS #69 B8)
        if (increment) {
            val episode = payload.episodeIndex
            progressInteractor.episodePlayed(payload.animeId, episode)
                    .subscribe({}, { Log.w(TAG, "could not count episode $episode", it) })
                    .addToDisposables()
        }

        interactor.saveTranslationSettings(TranslationSetting(payload.animeId, payload.author, payload.type))
                .subscribe({}, this::processErrors)
                .addToDisposables()
    }

    private fun getVideoAndExecute(payload: TranslationVideo, onSubscribe: (Video) -> Unit) = videoLookup.start {
        interactor.getVideo(payload)
                .appendLoadingLogic(viewState)
                .subscribe(onSubscribe::invoke, this::processVideoErrors)
    }

    /**
     * A failed lookup is a message, and the list stays (BUGS #60). An anti-bot check and a connection
     * or server error each say what happened; anything else reads as the video being gone, as an
     * unreadable hosting page does.
     */
    private fun processVideoErrors(throwable: Throwable) {
        val message = errorMessage(throwable)
        when {
            throwable is HostingChallengeException -> viewState.showHostingChallengeError()
            message != null -> viewState.showErrorMessage(message)
            else -> {
                Log.w(LOOKUP_TAG, "video lookup failed", throwable)
                viewState.showTracksNotFoundError()
            }
        }
    }

    fun onTrackForDownloadSelected(item: SeriesDownloadItem) {
        //Kodik urls used to be truncated at ".mp4" here to turn the hls manifest into a direct file,
        //because DownloadManager could only fetch one url. That file stopped existing - the shortened
        //url answers 500 - and DownloadService walks the playlist itself now, so the url is
        //passed through untouched.
        selectedDownload = item
        viewState.checkPermissions()
    }

    fun onStoragePermissionsAccepted() {
        val downloadPath = settingsSource.downloadFolder

        if (downloadPath.isNotEmpty()) selectedDownload?.let(this::downloadVideo)
        else viewState.showFolderChooserDialog()
    }

    private fun downloadVideo(item: SeriesDownloadItem) {
        val data = DownloadVideoData(
                //the video's own episode, not the screen's (BUGS #69 B8)
                navigationData.animeId, navigationData.name, item.video.episodeId.toInt(), item.track.url, item.track.audioUrl,
                parsers.headers(item.video),
                author = item.author,
                quality = item.quality,
                kind = item.kind,
                hosting = item.hosting
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

    /** How a list load ends. */
    private sealed class Loaded {
        class Translations(val items: List<TranslationViewModel>) : Loaded()

        /** The episode is out, but the source has nothing for it yet. */
        object NoTranslations : Loaded()

        /** No episode is out at all. */
        object NothingOut : Loaded()
    }

    companion object {
        private const val TAG = "WatchProgress"
        private const val LOOKUP_TAG = "VideoLookup"
    }
}
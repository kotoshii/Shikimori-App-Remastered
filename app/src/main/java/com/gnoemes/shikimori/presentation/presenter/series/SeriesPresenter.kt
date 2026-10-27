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

    //what the screen showed before Android killed the app in the background, if it did: set by the
    //screen from its saved state
    var restoredState: SeriesState? = null

    private var episode: Int? = null
    private var episodeId: Long? = null
    private var isAlternative: Boolean = settingsSource.altSourceByDefault
    private var setting: TranslationSetting? = null
    private var query: String? = null

    //the type this title was last watched in is read before its first translations; until then the
    //type button stays hidden, so a type picked by hand is never replaced by the saved one. A type
    //brought back from the saved state is known from the start
    private var isTypeKnown = false
    //the episode list is loading, so which episode will show is not known yet
    private var isLoadingEpisodes = false
    //no episode is out at all: neither the source nor the type can change that
    private var isNothingOut = false

    private val items = mutableListOf<TranslationViewModel>()
    private var selectedDownload: SeriesDownloadItem? = null

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
        val restored = restoredState
        type = restored?.type ?: settingsSource.translationType
        isTypeKnown = restored?.type != null || !settingsSource.useLocalTranslationSettings
        if (restored != null) {
            episode = restored.episode ?: navigationData.episode
            episodeId = restored.episodeId
            isAlternative = restored.isAlternative
        } else {
            //not cut to shikimori's count: the episode list tells whether it is out (BUGS #50)
            episode = navigationData.episode
        }
        //the episode list and the player read the count from here, and add what they write to it
        progressInteractor.remember(navigationData.animeId, navigationData.rateId, navigationData.watchedEpisodes)

        viewState.setBackground(navigationData.image)
        viewState.setTitle(navigationData.name)
        viewState.changeSource(isAlternative)
        //shikimori counts nothing out: so until the episode list says otherwise, as nothing out. The
        //controls showed during the load and went once it came (Test 72f)
        isNothingOut = navigationData.episodesAired <= 0
        viewState.showFab(!isNothingOut)

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
        //an episode not looked up yet: the episode list first, which tells whether the source has it
        else loadEpisodes()
    }

    //the next or the previous episode: its translations when the source has it, else that the source
    //does not have it. › reloaded the same episode when the source had no next one (BUGS #50)
    fun onNextEpisode() = loadEpisodes { episode = (episode ?: navigationData.episode) + 1 }

    fun onPreviousEpisode() = loadEpisodes { episode = ((episode ?: navigationData.episode) - 1).coerceAtLeast(1) }

    /**
     * The episode list, then [episode] from it (see [showEpisode]). [choose] changes [episode] once the
     * list has come, so a load that fails leaves the episode shown as it was.
     */
    private fun loadEpisodes(choose: () -> Unit = {}) = load(
            loadSettingsIfNeed()
                    //all the source has: shikimori's count can lag behind it (BUGS #50)
                    .flatMap { interactor.getEpisodes(navigationData.animeId, isAlternative) }
                    .doOnSuccess { choose() }
                    .flatMap { showEpisode(it) },
            loadsEpisodes = true)

    private fun translations(episodeId: Long): Single<Loaded> =
            loadSettingsIfNeed()
                    .flatMap { interactor.getTranslations(it, navigationData.animeId, episodeId, isAlternative, true) }
                    .map { Loaded.Translations(converter.convertTranslations(it, setting)) }

    //this title's saved choice - the type and the author last watched - read once, when that setting
    //is on. A type already known stays
    private fun loadSettingsIfNeed(): Single<TranslationType> =
            if (setting != null || !settingsSource.useLocalTranslationSettings) Single.just(type)
            else interactor.getTranslationSettings(navigationData.animeId)
                    .doOnSuccess {
                        setting = it
                        if (!isTypeKnown) type = it.lastType ?: settingsSource.translationType
                        isTypeKnown = true
                        showControls()
                    }
                    .map { type }

    /**
     * What the screen shows, for its saved state ([SeriesState]). Null before the screen has started:
     * a screen restored in a tab that was never opened again still keeps what it was given.
     */
    val state: SeriesState?
        get() = if (!::type.isInitialized) restoredState
        else SeriesState(episode, episodeId, isAlternative, type.takeIf { isTypeKnown })

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
     * that the source does not have it; else that nothing is out.
     */
    private fun showEpisode(items: List<Episode>): Single<Loaded> {
        val id = items.firstOrNull { it.index == episode }?.id
        episodeId = id
        isLoadingEpisodes = false
        isNothingOut = id == null && navigationData.episodesAired <= 0
        showControls()
        viewState.showFab(!isNothingOut)

        return when {
            id != null -> translations(id)
            isNothingOut -> Single.just(Loaded.NothingOut)
            else -> Single.just(Loaded.NotInSource)
        }
    }

    private fun onLoaded(loaded: Loaded) {
        when (loaded) {
            is Loaded.Translations -> setData(loaded.items)
            Loaded.NotInSource -> {
                viewState.hideEmptyView()
                viewState.showEpisodeNotFoundView(isAlternative)
                viewState.showContent(false)
            }
            Loaded.NothingOut -> viewState.showEmptyView()
        }
    }

    //whatever failed, the source can still be switched and «Повторить» tries again (BUGS #53)
    private fun onLoadFailed(throwable: Throwable) {
        isLoadingEpisodes = false
        //not known after all: the source button back, so the other source can be tried
        isNothingOut = false
        showControls()
        processErrors(throwable)
    }

    /**
     * Which toolbar controls show (BUGS #53): the source and the type whenever they can change
     * anything; ‹ «# N» › whenever an episode is out, a film too - the chip is the way to whatever the
     * source has beyond shikimori's count, such as a special uploaded as episode 25 of 24. The arrows
     * go by shikimori's count (P7-T5 Q117/Q118). While the episode list loads they stay, greyed out:
     * a tap there needs the episode, and hiding them moved the source button (Test 70f).
     */
    private fun showControls() {
        val last = navigationData.lastEpisode
        val controls = episode.takeIf { !isNothingOut }?.let {
            EpisodeControls(it, !isLoadingEpisodes, last > 1 || it > 1, it > 1, it < last)
        }
        viewState.showControls(!isNothingOut, type.takeIf { isTypeKnown && !isNothingOut }, controls)
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
        //A video whose sound is its own file is shared as its manifest, so the link plays with sound;
        //the manifest holds every quality, so the text names none (BUGS #56)
        val manifestUrl = item.track.manifestUrl
        val videoUrl = manifestUrl ?: item.track.url
        val quality = if (manifestUrl != null) "" else item.quality

        //the same four things the downloaded file is named after, so a shared link and a saved file
        //describe the episode alike. Blanks drop out rather than leaving separators behind
        val details = listOf(item.author, item.kind, quality, item.hosting).filter { it.isNotBlank() }

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
        else viewState.showDownloadHostingDialog(converter.authorLabel(filteredItems.first().author), filteredItems, filteredItems.size < videos.size)
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
            else viewState.showDownloadDialog(converter.authorLabel(video.author), items)
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

    fun onHostingClicked(video: TranslationVideo) {
        //what the built-in player needs goes into the picker with the video, so a tap there still works
        //after Android has killed the app and brought the picker back (BUGS #48, T5-35)
        val episodesSize = items.firstOrNull()?.episodesSize ?: return
        val playing = EmbeddedPlayerNavigationData(navigationData.name, episodesSize, video, isAlternative)

        if (!parsers.isSupported(video.videoHosting)) openVideo(playing, PlayerType.WEB)
        else if (!settingsSource.isAskForPlayer) openVideo(playing, settingsSource.playerType)
        else viewState.showPlayerDialog(playing)
    }

    //«Запомнить выбор» keeps the player picked and stops asking (BUGS #33)
    fun onPlayerSelected(playing: EmbeddedPlayerNavigationData, playerType: PlayerType, remember: Boolean) {
        if (remember) {
            settingsSource.playerType = playerType
            settingsSource.isAskForPlayer = false
        }
        openVideo(playing, playerType)
    }

    //Only embedded player can process object payload
    //Others o uses urls
    private fun openVideo(playing: EmbeddedPlayerNavigationData, playerType: PlayerType) {
        val video = playing.payload

        //anime365 resolves nothing without a login, so say so instead of opening a player that will
        //find no tracks. The web player is the exception - it loads anime365's own page, which asks
        //for the login itself
        if (playerType != PlayerType.WEB && needsAnime365Login(video)) {
            viewState.showAnime365LoginRequired()
            return
        }

        if (playerType == PlayerType.EMBEDDED) play(video, playerType, playing)
        else if (playerType == PlayerType.WEB && video.webPlayerUrl != null) play(video, playerType, video.webPlayerUrl)
        else getVideoAndExecute(video) { showQualityChooser(video, playerType, it) }
    }

    /** Anime365 needs a paid account, and without the token nothing it hands back has tracks. */
    private fun needsAnime365Login(video: TranslationVideo): Boolean =
            video.videoHosting is VideoHosting.SMOTRET_ANIME && tokenSource.getToken() == null

    private fun showQualityChooser(video: TranslationVideo, playerType: PlayerType, parsed: Video) {
        val tracks = parsed.tracks
        if (tracks.isEmpty()) {
            viewState.showTracksNotFoundError()
            return
        }
        //an outside player gets a manifest when the sound is its own file, and the manifest holds every
        //quality: there is nothing to choose (BUGS #56)
        val oneChoice = tracks.size == 1 || playerType == PlayerType.EXTERNAL && tracks.first().manifestUrl != null
        if (oneChoice || settingsSource.isExternalBestQuality) onQualitySelected(video, playerType, parsed, tracks.first())
        else viewState.showQualityChooser(video, playerType, parsed)
    }

    fun onQualitySelected(video: TranslationVideo, playerType: PlayerType, parsed: Video, track: Track) {
        //another app may not be there to open it: the screen tries, and says when one did
        if (playerType == PlayerType.EXTERNAL) viewState.openExternalPlayer(video, ExternalPlayerData.of(track, parsers.playerHeaders(parsed)))
        else play(video, playerType, track.url)
    }

    /** Opens [video] in the built-in or the web player: [payload] is the built-in player's data, else the url to play. */
    private fun play(video: TranslationVideo, playerType: PlayerType, payload: Any?) {
        openPlayer(playerType, payload)
        onPlayerOpened(video, playerType)
    }

    //«Внешний плеер» counts the episode only once an app has opened it (BUGS #63)
    fun onExternalPlayerOpened(video: TranslationVideo) = onPlayerOpened(video, PlayerType.EXTERNAL)

    private fun onPlayerOpened(video: TranslationVideo, playerType: PlayerType) {
        isWatchSession = true
        saveSettingsAndIncrementOptional(playerType != PlayerType.EMBEDDED, video)
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

        /** The source does not list the episode: not out yet, or not uploaded there. */
        object NotInSource : Loaded()

        /** No episode is out at all. */
        object NothingOut : Loaded()
    }

    companion object {
        private const val TAG = "WatchProgress"
        private const val LOOKUP_TAG = "VideoLookup"
    }
}
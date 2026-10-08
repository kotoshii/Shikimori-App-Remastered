package com.gnoemes.shikimori.presentation.presenter.player

import android.util.Log
import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.repository.series.shikimori.parser.HostingParsers
import com.gnoemes.shikimori.domain.series.SeriesInteractor
import com.gnoemes.shikimori.domain.series.WatchProgressInteractor
import com.gnoemes.shikimori.entity.app.domain.exceptions.HostingChallengeException
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.EmbeddedPlayerNavigationData
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.view.player.embedded.EmbeddedPlayerView
import com.gnoemes.shikimori.presentation.view.player.embedded.provider.EmbeddedPlayerResourceProvider
import com.gnoemes.shikimori.utils.appendLoadingLogic
import io.reactivex.disposables.Disposable
import javax.inject.Inject

@InjectViewState
class EmbeddedPlayerPresenter @Inject constructor(
        private val interactor: SeriesInteractor,
        private val progressInteractor: WatchProgressInteractor,
        private val resourceProvider: EmbeddedPlayerResourceProvider,
        private val parsers: HostingParsers
) : BaseNetworkPresenter<EmbeddedPlayerView>() {

    lateinit var navigationData: EmbeddedPlayerNavigationData

    private var currentEpisode: Int = -1
    private var currentTrack = 0
    private lateinit var payload: TranslationVideo

    //the episode prev/next is loading, if any. A further tap counts from it, so two fast taps skip two
    //episodes ahead instead of loading the same one twice. Back to null once the load ends, either way
    private var pendingEpisode: Int? = null
    private var episodeDisposable: Disposable? = null

    private val videos = hashSetOf<Video>()

    override fun initData() {
        super.initData()
        currentEpisode = navigationData.payload.episodeIndex
        payload = navigationData.payload

        viewState.setTitle(navigationData.animeName)

        loadEpisodeVideo(currentEpisode, payload)
        updateControls()
    }

    private fun loadTranslations(type: TranslationType, episodeId: Long) = interactor
            .getTranslations(type, animeId, episodeId, navigationData.isAlternative, false)
            .appendLoadingLogic(viewState)

    //only videos with tracks get here, see loadEpisodeVideo
    private fun updateVideo(video: Video, needReset: Boolean = true) {
        videos.add(video)
        setTrack(video, needReset)
    }

    private fun setTrack(video: Video, needReset: Boolean) {
        val track = video.tracks.getOrNull(currentTrack)
        track?.let {
            viewState.apply {
                setEpisodeSubtitle(currentEpisode)
                playVideo(it, video.subAss, needReset, parsers.headers(video))
                val resolutions = video.tracks.asSequence().filter { it.quality != "unknown" }.map { it.quality }.toList()
                setResolutions(resolutions)
                selectTrack(currentTrack)
            }
            currentTrack = 0
            setEpisodeWatched()
        } ?: viewState.showMessage(resourceProvider.playerErrorMessage)
    }

    private fun updateControls() {
        viewState.enableNextButton(currentEpisode < navigationData.episodesSize)
        viewState.enablePrevButton(currentEpisode > 1)
    }

    //«Автоматический подсчет», which decides for itself whether this episode counts. Only logged on
    //failure - the episode is playing, and an error over it would help nobody
    private fun setEpisodeWatched() {
        val episode = currentEpisode
        progressInteractor.episodePlayed(animeId, episode)
                .subscribe({}, { Log.w(TAG, "could not count episode $episode", it) })
                .addToDisposables()
    }

    fun loadNextEpisode() = loadEpisode((pendingEpisode ?: currentEpisode) + 1)

    fun loadPrevEpisode() = loadEpisode((pendingEpisode ?: currentEpisode) - 1)

    //currentEpisode moves only once the new episode's video is in hand, so a failed switch leaves the
    //title, the buttons and the watched mark on the episode that is still playing
    private fun loadEpisode(episode: Int) {
        if (episode < 1 || episode > navigationData.episodesSize) return

        //a tap replaces whatever was loading, the episode the player opened on included - otherwise a
        //slow load lands on top of the episode the user has since switched to
        episodeDisposable?.dispose()
        pendingEpisode = episode

        val video = videos.find { it.episodeId.toInt() == episode }
        if (video != null) {
            //appendLoadingLogic hides the progress bar when its chain ends, which disposing skips, and
            //an already loaded episode starts no new chain to hide it
            viewState.onHideLoading()
            showEpisode(episode, video)
            return
        }

        episodeDisposable = loadTranslations(navigationData.payload.type, episode.toLong())
                .subscribe({ translations ->
                    val translation = translations.find {
                        if (payload.author.isNotEmpty()) it.author == payload.author && it.hosting == payload.videoHosting
                        else it.author.isEmpty() && it.hosting == payload.videoHosting
                    }

                    if (translation != null) loadEpisodeVideo(episode, payload.copy(videoId = translation.videoId, episodeIndex = episode, webPlayerUrl = translation.webPlayerUrl))
                    else {
                        pendingEpisode = null
                        viewState.showMessage(resourceProvider.translationNotFound)
                    }
                }, { processVideoErrors(episode, it) })
                .also { it.addToDisposables() }
    }

    private fun loadEpisodeVideo(episode: Int, newPayload: TranslationVideo) {
        episodeDisposable = interactor.getVideo(newPayload)
                .appendLoadingLogic(viewState)
                .subscribe({
                    if (it.tracks.isEmpty()) onNothingToPlay()
                    else {
                        payload = newPayload
                        showEpisode(episode, it)
                    }
                }, { processVideoErrors(episode, it) })
                .also { it.addToDisposables() }
    }

    private fun showEpisode(episode: Int, video: Video) {
        pendingEpisode = null
        currentEpisode = episode
        updateVideo(video)
        updateControls()
    }

    //the same words the series screen uses when a hosting has nothing to play. Checked before
    //showEpisode, so a switch to such an episode keeps the one that is playing, as a failed load does;
    //only when nothing has played yet is there nothing to stay for
    private fun onNothingToPlay() {
        pendingEpisode = null
        viewState.showMessage(resourceProvider.videoNotFoundMessage, videos.isEmpty())
    }

    //for the first video and for a switch alike. This screen's navigator is a no-op, so
    //BaseNetworkPresenter.processErrors would show nothing: say the video could not be loaded and stay,
    //so whatever is playing keeps playing and prev/next still work. An anti-bot check is not the video
    //being gone, and the player cannot get past it, so that one says what happened and leaves
    private fun processVideoErrors(episode: Int, throwable: Throwable) {
        pendingEpisode = null
        Log.w(TAG, "loading episode $episode failed", throwable)
        if (throwable is HostingChallengeException) viewState.showMessage(resourceProvider.hostingChallengeMessage, true)
        else viewState.showMessage(resourceProvider.playerErrorMessage)
    }

    fun onResolutionChanged(newResolution: String) {
        val video = videos.find { it.episodeId.toInt() == currentEpisode }
        val track = video?.tracks?.find { it.quality == newResolution }
        track?.let {
            currentTrack = video.tracks.indexOf(it)
            updateVideo(video, false)
        }
    }

    private val animeId: Long
        get() = navigationData.payload.animeId

    companion object {
        private const val TAG = "EmbeddedPlayer"
    }

}
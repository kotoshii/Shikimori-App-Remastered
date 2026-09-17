package com.gnoemes.shikimori.presentation.presenter.player

import android.util.Log
import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.domain.series.SeriesInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.app.domain.HttpStatusCode
import com.gnoemes.shikimori.entity.app.domain.exceptions.HostingChallengeException
import com.gnoemes.shikimori.entity.app.domain.exceptions.ServiceCodeException
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.EmbeddedPlayerNavigationData
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.view.player.embedded.EmbeddedPlayerView
import com.gnoemes.shikimori.presentation.view.player.embedded.provider.EmbeddedPlayerResourceProvider
import com.gnoemes.shikimori.utils.Utils
import com.gnoemes.shikimori.utils.appendLoadingLogic
import javax.inject.Inject

@InjectViewState
class EmbeddedPlayerPresenter @Inject constructor(
        private val interactor: SeriesInteractor,
        private val settingsSource: SettingsSource,
        private val resourceProvider: EmbeddedPlayerResourceProvider
) : BaseNetworkPresenter<EmbeddedPlayerView>() {

    lateinit var navigationData: EmbeddedPlayerNavigationData

    private var currentEpisode: Int = -1
    private var currentTrack = 0
    private lateinit var payload: TranslationVideo

    private val videos = hashSetOf<Video>()

    override fun initData() {
        super.initData()
        currentEpisode = navigationData.payload.episodeIndex
        payload = navigationData.payload

        viewState.setTitle(navigationData.animeName)

        loadVideo(payload)
        updateControls()
    }

    private fun loadVideo(payload: TranslationVideo) {
        interactor.getVideo(payload)
                .appendLoadingLogic(viewState)
                .subscribe({ updateVideo(it) }, this::processLoadVideoErrors)
                .addToDisposables()
    }

    private fun loadTranslations(type: TranslationType, episodeId: Long) = interactor
            .getTranslations(type, animeId, episodeId, navigationData.nameEng, navigationData.isAlternative, false)
            .appendLoadingLogic(viewState)

    private fun updateVideo(video: Video, needReset: Boolean = true) {
        videos.add(video)

        if (!Utils.isHostingSupports(video.hosting)) viewState.showMessage(resourceProvider.hostingErrorMessage)
        else if (video.tracks.isNotEmpty()) setTrack(video, needReset)
        else viewState.showMessage(resourceProvider.playerErrorMessage, true)
    }

    private fun setTrack(video: Video, needReset: Boolean) {
        val track = video.tracks.getOrNull(currentTrack)
        track?.let {
            viewState.apply {
                setEpisodeSubtitle(currentEpisode)
                playVideo(it, video.subAss, needReset, Utils.getRequestHeadersForHosting(video))
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

    private fun setEpisodeWatched() {
        if (!settingsSource.isAutoIncrement) return
        val rateId = navigationData.rateId ?: Constants.NO_ID
        interactor
                .sendEpisodeChanges(EpisodeChanges.Changes(rateId, animeId, currentEpisode, true))
                .subscribe({}, this::processErrors)
                .addToDisposables()
    }

    private fun processLoadVideoErrors(throwable: Throwable) {
        //an anti-bot check is not the video being gone, and the player cannot get past it, so say
        //what happened and leave rather than sit on an empty screen
        if (throwable is HostingChallengeException) {
            viewState.showMessage(resourceProvider.hostingChallengeMessage, true)
        } else if (throwable is ServiceCodeException && throwable.serviceCode == HttpStatusCode.NOT_FOUND) {
            viewState.showMessage(resourceProvider.playerErrorMessage)
        } else super.processErrors(throwable)
    }

    fun loadNextEpisode() = loadEpisode(currentEpisode + 1)

    fun loadPrevEpisode() = loadEpisode(currentEpisode - 1)

    //currentEpisode moves only once the new episode's video is in hand, so a failed switch leaves the
    //title, the buttons and the watched mark on the episode that is still playing
    private fun loadEpisode(episode: Int) {
        val video = videos.find { it.episodeId.toInt() == episode }

        if (video != null) {
            showEpisode(episode, video)
            return
        }

        loadTranslations(navigationData.payload.type, episode.toLong())
                .subscribe({ translations ->
                    val translation = translations.find {
                        if (payload.author.isNotEmpty()) it.author == payload.author && it.hosting == payload.videoHosting
                        else it.author.isEmpty() && it.hosting == payload.videoHosting
                    }

                    if (translation != null) loadEpisodeVideo(episode, payload.copy(videoId = translation.videoId, episodeIndex = episode, webPlayerUrl = translation.webPlayerUrl))
                    else viewState.showMessage(resourceProvider.translationNotFound)
                }, { processEpisodeErrors(episode, it) })
                .addToDisposables()
    }

    private fun loadEpisodeVideo(episode: Int, newPayload: TranslationVideo) {
        interactor.getVideo(newPayload)
                .appendLoadingLogic(viewState)
                .subscribe({
                    payload = newPayload
                    showEpisode(episode, it)
                }, { processEpisodeErrors(episode, it) })
                .addToDisposables()
    }

    private fun showEpisode(episode: Int, video: Video) {
        currentEpisode = episode
        updateVideo(video)
        updateControls()
    }

    //this screen's navigator is a no-op, so BaseNetworkPresenter.processErrors would show nothing -
    //the episode that was playing keeps playing and the user is told the switch failed
    private fun processEpisodeErrors(episode: Int, throwable: Throwable) {
        Log.w(TAG, "switching to episode $episode failed", throwable)
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
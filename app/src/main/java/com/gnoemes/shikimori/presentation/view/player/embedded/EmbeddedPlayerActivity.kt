package com.gnoemes.shikimori.presentation.view.player.embedded

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.text.SpannableStringBuilder
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.PlayerSettingsSource
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.VideoFormat
import com.gnoemes.shikimori.presentation.presenter.player.EmbeddedPlayerPresenter
import com.gnoemes.shikimori.presentation.view.base.activity.BaseActivity
import com.gnoemes.shikimori.presentation.view.common.widget.CustomSpinner
import com.gnoemes.shikimori.presentation.view.common.widget.ReSpinner
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.exoplayer.MediaSourceHelper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.android.synthetic.main.activity_embedded_player.*
import kotlinx.android.synthetic.main.layout_player_bottom.*
import kotlinx.android.synthetic.main.layout_player_controls.*
import kotlinx.android.synthetic.main.layout_player_rewind_forward.*
import kotlinx.android.synthetic.main.layout_player_toolbar.*
import org.joda.time.Duration
import ru.terrakok.cicerone.Navigator
import ru.terrakok.cicerone.NavigatorHolder
import javax.inject.Inject
import kotlin.math.absoluteValue

class EmbeddedPlayerActivity : BaseActivity<EmbeddedPlayerPresenter, EmbeddedPlayerView>(), EmbeddedPlayerView {

    @InjectPresenter
    lateinit var playerPresenter: EmbeddedPlayerPresenter

    @ProvidePresenter
    fun providePresenter(): EmbeddedPlayerPresenter =
            presenterProvider.get()
                    .apply { navigationData = intent.getParcelableExtra(AppExtras.ARGUMENT_PLAYER_DATA) }

    @Inject
    lateinit var localNavigatorHolder: NavigatorHolder

    @Inject
    lateinit var settingsSource: PlayerSettingsSource

    companion object {
        private const val DEFAULT_USER_AGENT = "sap"
        const val CONTROLLER_HIDE_DELAY = 3500L
        const val UNLOCK_HIDE_DELAY = 2000L
        const val FORWARD_REWIND_HIDE_DELAY = 750L
    }

    override var applyTheme: Boolean
        get() = false
        set(value) {}

    private val controller by lazy { PlayerController(settingsSource) }
    private val screen by lazy { PlayerScreen(this) }
    private val levels by lazy { PlayerLevels(this) }

    private val smallOffsetText by lazy { "${settingsSource.forwardRewindOffset / 1000} ${getString(R.string.player_seconds_short)}" }
    private val bigOffsetText by lazy { "${settingsSource.forwardRewindOffsetBig / 1000} ${getString(R.string.player_seconds_short)}" }
    private val brightnessIcon by lazy { drawable(R.drawable.ic_brightness) }
    private val volumeIcon by lazy { drawable(R.drawable.ic_volume) }
    private val offsetColor by lazy { color(R.color.player_unlock_background) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.ShikimoriAppTheme_Player)
        theme.applyStyle(getCurrentAscentTheme, true)
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        toolbar.addBackButton(icon = R.drawable.ic_arrow_back_player) { onBackPressed() }

        if (settingsSource.isOpenLandscape) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        forwardView.text = smallOffsetText
        forwardView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_forward, 0, 0)
        rewindView.text = smallOffsetText
        rewindView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_rewind, 0, 0)

        exo_progress.setBufferedColor(ColorUtils.setAlphaComponent(colorAttr(R.attr.colorSecondaryTransparent), 153))
        resolutionSpinnerView.background.tint(color(R.color.player_controls))

        speedSpinnerView.apply {
            background.tint(color(R.color.player_controls))
            adapter = ArrayAdapter(this@EmbeddedPlayerActivity, R.layout.item_spinner_player, resources.getStringArray(R.array.player_speed_rates))
            setOnItemClickListener { _, _, position, _ -> controller.changePlaySpeed(position) }
            setSelection(2, false)
        }
        includedToolbar.gone()
        unlockView.hide()

        pipView.visibleIf { screen.hasPip }

        prev.onClick { presenter.loadPrevEpisode() }
        next.onClick { presenter.loadNextEpisode() }
        rotationView.onClick { screen.toggleOrientation() }
        lockView.onClick { controller.lock() }
        unlockView.onClick { controller.unlock() }
        pipView.onClick { screen.enterPip() }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (settingsSource.isAutoPip)
            screen.enterPip()
    }

    override fun onStart() {
        super.onStart()
        controller.onStart()
    }

    override fun onStop() {
        super.onStop()
        controller.onStop()
    }

    override fun onDestroy() {
        controller.destroy()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            screen.hideSystemUi()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        screen.onConfigurationChanged()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        playerView.useController = !isInPictureInPictureMode
    }

    private fun changeProgressSlide(current: Long, max: Long, offset: Long) {
        val validatedOffset = when {
            current + offset < 0 -> current
            current + offset > max -> max - current
            else -> offset
        }

        val validatedCurrent = when {
            current + offset < 0 -> 0
            current + offset > max -> max
            else -> current + offset
        }

        val offsetText = toMinutesAndSecond(validatedOffset)?.let { timeOffset ->
            (if (offset > 0) "+" else "-").plus(timeOffset)
        }
        val maxTimeOffset = toMinutesAndSecond(max)
        val validatedCurrentTimeOffset = toMinutesAndSecond(validatedCurrent)

        if (maxTimeOffset == null || validatedCurrentTimeOffset == null || offsetText == null) return

        val text = "$validatedCurrentTimeOffset / $maxTimeOffset "

        val spanString = SpannableStringBuilder(text)
                .append(offsetText.colorSpan(offsetColor))

        showParamChanges(spanString, null)
    }

    private fun showParamChanges(text: CharSequence, icon: Drawable?) {
        paramChangesView.text = text
        paramChangesView.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        paramChangesView.visible()
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getLayoutActivity(): Int = R.layout.activity_embedded_player

    override fun getNavigator(): Navigator = Navigator { }

    override fun getNavigatorHolder(): NavigatorHolder = localNavigatorHolder

    override val presenter: EmbeddedPlayerPresenter
        get() = playerPresenter

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun setTitle(title: String) {
        toolbar.title = title
    }

    override fun setEpisodeSubtitle(currentEpisode: Int) {
        val subTitle = String.format(getString(R.string.episode_number), currentEpisode)
        toolbar.subtitle = subTitle
    }

    override fun setResolutions(resolutions: List<String>) {
        resolutionSpinnerView.visibleIf { resolutions.isNotEmpty() }
        if (resolutions.isNotEmpty()) {
            resolutionSpinnerView.adapter = ArrayAdapter(this, R.layout.item_spinner_player, resolutions)
            resolutionSpinnerView.setOnItemClickListener { _, _, position, _ -> presenter.onResolutionChanged(resolutions[position]) }
        }
    }

    override fun onBackPressed() {
        super.finish()
    }

    override fun selectTrack(position: Int) = controller.selectTrack(position)
    override fun enableNextButton(enable: Boolean) = controller.enableNextButton(enable)
    override fun enablePrevButton(enable: Boolean) = controller.enablePrevButton(enable)
    override fun onShowLoading() = progressBar.visible()
    override fun onHideLoading() = progressBar.gone()
    override fun onShowLightLoading() = bufferingProgressBar.visible()
    override fun onHideLightLoading() = bufferingProgressBar.gone()

    override fun showMessage(s: String, exit: Boolean) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
        if (exit) onBackPressed()
    }

    override fun playVideo(it: Track, subtitles: String?, needReset: Boolean, headers: Map<String, String>) {
        //some hostings sign their links for a specific user agent, so it has to win over the default
        val userAgent = headers["User-Agent"] ?: DEFAULT_USER_AGENT
        val factory = DefaultHttpDataSourceFactory(userAgent, DefaultBandwidthMeter(), Constants.LONG_TIMEOUT * 1000, Constants.LONG_TIMEOUT * 1000, true)
        factory.defaultRequestProperties.set(headers)

        val source = MediaSourceHelper
                .withFactory(factory)
                .apply {
                    if (it.url.contains("m3u8")) withFormat(VideoFormat.HLS)
                    else withFormat(VideoFormat.MP4)
                }
                .withVideoUrl(it.url)
                .withAudioUrl(it.audioUrl)
                .withSubtitles(subtitles, Format.createTextSampleFormat(null, MimeTypes.TEXT_SSA, Format.NO_VALUE, null))
                .get()

        if (needReset) controller.addMediaSource(source)
        else controller.updateTrack(source)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Player controller
    ///////////////////////////////////////////////////////////////////////////

    /** ExoPlayer, the media session and the controls; the touch gestures are [PlayerGestureHandler]'s. */
    private inner class PlayerController(
            private val settingsSource: PlayerSettingsSource
    ) : Player.EventListener, PlayerControlView.VisibilityListener, PlayerGestureHandler.Callback {

        private val player: SimpleExoPlayer

        override var isLocked: Boolean = false
        private var controlsVisibility = View.GONE
        private val playerMargin by lazy { dimenAttr(android.R.attr.actionBarSize) }

        private val smallOffset by lazy { settingsSource.forwardRewindOffset }
        private val bigOffset by lazy { settingsSource.forwardRewindOffsetBig }
        private val isZoomProportional by lazy { settingsSource.isZoomProportional }

        private val speedRates = listOf(0.25f, 0.5f, 1f, 1.25f, 1.5f, 2f)
        private var controlsInAction: Boolean = false

        private val connector: MediaSessionConnector

        private val progressListener = object : TimeBar.OnScrubListener {
            private var prevPosition = 0L

            override fun onScrubStart(timeBar: TimeBar?, position: Long) {
                prevPosition = position
                controlsInAction = true
            }

            override fun onScrubStop(timeBar: TimeBar?, position: Long, canceled: Boolean) {
                prevPosition = if (!canceled) position else 0L
                onActionEnd()
            }

            override fun onScrubMove(timeBar: TimeBar?, position: Long) {
                val dragText = (if (prevPosition < position) "+" else "-").plus(" ${toMinutesAndSecond(prevPosition - position)}")
                dragProgress.apply {
                    text = dragText
                    visible()
                    removeCallbacks(dragPostHideRunnable)
                    postDelayed(dragPostHideRunnable, CONTROLLER_HIDE_DELAY)
                }
            }
        }

        private val spinnerOpenListener = object : CustomSpinner.OnSpinnerEventsListener {
            override fun onSpinnerOpened(spinner: ReSpinner?) {
                controlsInAction = true
            }

            override fun onSpinnerClosed(spinner: ReSpinner?) = onActionEnd()
        }

        init {
            val trackSelector = DefaultTrackSelector(AdaptiveTrackSelection.Factory())
            player = ExoPlayerFactory.newSimpleInstance(this@EmbeddedPlayerActivity, trackSelector)
            player.addListener(this)
            playerView.player = player
            playerView.setControllerShowOnTouch(false)
            playerView.setControllerVisibilityListener(this)
            playerView.controllerAutoShow = false
            playerView.setOnTouchListener(PlayerGestureHandler(this@EmbeddedPlayerActivity, playerView, playerMargin, settingsSource, this))
            exo_progress.addListener(progressListener)
            resolutionSpinnerView.setSpinnerEventsListener(spinnerOpenListener)
            speedSpinnerView.setSpinnerEventsListener(spinnerOpenListener)

            val mediaSession = MediaSessionCompat(this@EmbeddedPlayerActivity, packageName)
            connector = MediaSessionConnector(mediaSession)
        }

        val isVisible
            get() = controlsVisibility == View.VISIBLE

        fun enableNextButton(enable: Boolean) {
            val alpha =
                    if (!enable) 0.3f
                    else 1f
            next.alpha = alpha
            next.isEnabled = enable
        }

        fun enablePrevButton(enable: Boolean) {
            val alpha =
                    if (!enable) 0.3f
                    else 1f
            prev.alpha = alpha
            prev.isEnabled = enable
        }

        fun selectTrack(position: Int) {
            resolutionSpinnerView.setSelection(position, false)
        }

        fun changePlaySpeed(currentSpeed: Int) {
            player.playbackParameters = PlaybackParameters(speedRates[currentSpeed])
            speedSpinnerView.setSelection(currentSpeed, false)
        }

        fun addMediaSource(source: MediaSource?) {
            player.playWhenReady = true
            player.prepare(source)
        }

        fun updateTrack(source: MediaSource?) {
            player.playWhenReady = true
            player.prepare(source, false, false)
        }

        fun onStart() {
            connector.setPlayer(player, null)
            connector.mediaSession.isActive = true
        }

        fun onStop() {
            player.playWhenReady = false
            connector.setPlayer(null, null)
            connector.mediaSession.isActive = false
        }

        fun destroy() {
            player.stop()
            player.release()
            connector.mediaSession.release()
        }

        fun lock() {
            isLocked = true
            screen.holdOrientation(locked = true)
            onLockedTouch()
        }

        fun unlock() {
            isLocked = false
            TransitionManager.beginDelayedTransition(container, Fade(Fade.MODE_OUT))
            unlockSurface.gone()
            unlockView.hide()
            screen.holdOrientation(locked = false)
        }

        private fun seek(pos: Long) {
            var mills = pos
            if (mills >= 0 || player.currentPosition != 0L) {
                mills += player.currentPosition

                if (mills < 0) mills = 0
                if (mills > player.duration) mills = player.duration

                player.seekTo(mills)
            }
        }

        override fun onVisibilityChange(visibility: Int) {
            controlsVisibility = visibility

            if (isVisible && !controlsInAction) {
                hideAfterTimeout()
                includedToolbar.visible()
            } else if (!controlsInAction) {
                includedToolbar.gone()
            }
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            error?.printStackTrace()
            showMessage(getString(R.string.player_error))
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> onShowLightLoading()
                else -> onHideLightLoading()
            }
        }

        ///////////////////////////////////////////////////////////////////////////
        // Gestures
        ///////////////////////////////////////////////////////////////////////////

        override fun onTap() {
            if (isVisible && !controlsInAction) playerView.hideController()
            else if (!controlsInAction) playerView.showController()
        }

        override fun onLockedTouch() {
            unlockView.show()
            TransitionManager.beginDelayedTransition(container, Fade(Fade.MODE_IN))
            unlockSurface.visible()
            playerView.hideController()
            hideUnlockAfterTimeout()
        }

        override fun onSeekTap(forward: Boolean, big: Boolean) {
            val offset = if (big) bigOffset else smallOffset
            val text = if (big) bigOffsetText else smallOffsetText

            if (forward) {
                forwardView.text = text
                seek(offset)
                hideForwardAfterTimeout()
            } else {
                rewindView.text = text
                seek(-offset)
                hideRewindAfterTimeout()
            }
        }

        override fun onBrightnessStep(step: Int) = showParamChanges("${levels.changeBrightness(step)}%", brightnessIcon)

        override fun onVolumeStep(step: Int) = showParamChanges("${levels.changeVolume(step)}%", volumeIcon)

        override fun onSeekSwipe(offset: Long) = changeProgressSlide(player.currentPosition, player.duration, offset)

        override fun onSwipeEnd(seekOffset: Long?) {
            paramChangesView.removeCallbacks(delayedParamChangesHide)
            paramChangesView.postDelayed(delayedParamChangesHide, UNLOCK_HIDE_DELAY)
            levels.onSwipeEnd()

            if (seekOffset != null) seek(seekOffset)
        }

        override fun onZoom(zoomed: Boolean) {
            playerView.resizeMode =
                    if (!zoomed) AspectRatioFrameLayout.RESIZE_MODE_FIT
                    else if (isZoomProportional) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else AspectRatioFrameLayout.RESIZE_MODE_FILL
        }

        private fun onActionEnd() {
            controlsInAction = false
            hideAfterTimeout()
        }

        private fun hideForwardAfterTimeout() {
            forwardView.apply {
                removeCallbacks(delayedForwardHide)
                visible()
                postDelayed(delayedForwardHide, FORWARD_REWIND_HIDE_DELAY)
            }
        }

        private fun hideRewindAfterTimeout() {
            rewindView.apply {
                removeCallbacks(delayedRewindHide)
                visible()
                postDelayed(delayedRewindHide, FORWARD_REWIND_HIDE_DELAY)
            }
        }

        private fun hideAfterTimeout() {
            playerView.removeCallbacks(postHideRunnable)
            playerView.postDelayed(postHideRunnable, CONTROLLER_HIDE_DELAY)
        }

        private fun hideUnlockAfterTimeout() {
            unlockView.removeCallbacks(delayedUnlockHide)
            unlockView.postDelayed(delayedUnlockHide, UNLOCK_HIDE_DELAY)
        }

        private val postHideRunnable = Runnable { if (!controlsInAction) playerView.hideController() }
        private val delayedForwardHide = Runnable { forwardView.gone() }
        private val delayedRewindHide = Runnable { rewindView.gone() }
        private val delayedUnlockHide = Runnable { unlockView.hide(); TransitionManager.beginDelayedTransition(container, Fade(Fade.MODE_OUT)); unlockSurface.gone() }
        private val delayedParamChangesHide = Runnable { TransitionManager.beginDelayedTransition(container, Fade(Fade.MODE_OUT)); paramChangesView.gone() }
        private val dragPostHideRunnable = Runnable { dragProgress.gone(); dragProgress.text = null }
    }

    private fun toMinutesAndSecond(value: Long): String? {
        if (value.absoluteValue !in 0..Int.MAX_VALUE) return null

        return Duration.millis(Math.abs(value)).toMinutesAndSeconds()
    }
}

package com.gnoemes.shikimori.presentation.view.player.embedded

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.gnoemes.shikimori.data.local.preference.PlayerSettingsSource
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The player's touch gestures, as the settings allow them. A tap shows or hides the controls; a
 * double tap or a long press on a side seeks; a vertical swipe on a side changes the brightness or
 * the volume, or a horizontal swipe seeks; a pinch zooms. [callback] does the work.
 *
 * Touches on the top strip and the right edge are left to the controls there; [edge] is their size.
 */
class PlayerGestureHandler(
        context: Context,
        private val view: View,
        private val edge: Int,
        settings: PlayerSettingsSource,
        private val callback: Callback
) : GestureDetector.SimpleOnGestureListener(), View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {

    interface Callback {
        val isLocked: Boolean

        /** Show or hide the controls. */
        fun onTap()

        fun onLockedTouch()

        /** A double tap ([big] false) or a long press ([big] true) on a side. */
        fun onSeekTap(forward: Boolean, big: Boolean)

        fun onBrightnessStep(step: Int)
        fun onVolumeStep(step: Int)

        /** A horizontal swipe, [offset] ms away. */
        fun onSeekSwipe(offset: Long)

        /** The finger is up after a swipe; [seekOffset] is set after a horizontal one. */
        fun onSwipeEnd(seekOffset: Long?)

        fun onZoom(zoomed: Boolean)
    }

    private val gestures = GestureDetector(context, this)
    private val scale = ScaleGestureDetector(context, this)

    private val isGesturesEnabled = settings.isGesturesEnabled
    private val isLevelsEnabled = settings.isVolumeAndBrightnessGesturesEnabled
    private val isLevelsInverted = settings.isVolumeAndBrightnessInverted
    private val isSlideControl = settings.isForwardRewindSlide

    private var isDrag: Boolean = false
    private var isSlide: Boolean = false
    private var slideOffset = 0L
    private var lastTapMills = 0L
    private var scaleFactor = 0.5f
    private var scrollFirstTapX = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event == null || event.y < edge / 2 || event.x > view.width - edge) return false

        if (callback.isLocked) {
            callback.onLockedTouch()
            return false
        }

        if (event.action == MotionEvent.ACTION_UP && isDrag) {
            isDrag = false
            onScrollEnd()
        }

        return if (isGesturesEnabled) {
            scale.onTouchEvent(event)
            gestures.onTouchEvent(event)
        } else if ((event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_DOWN) && System.currentTimeMillis() - lastTapMills > TAP_INTERVAL) {
            lastTapMills = System.currentTimeMillis()
            callback.onTap()
            true
        } else false
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        callback.onTap()
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
        if (e == null || isSlideControl) return
        val forward = side(e) ?: return
        callback.onSeekTap(forward, big = true)
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        if (e == null || isSlideControl) return false
        val forward = side(e) ?: return false
        callback.onSeekTap(forward, big = false)
        return true
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        if (e1 == null || e2 == null) return false

        return if (isLevelsEnabled) {
            val diff = abs(e1.y - e2.y).toInt()
            isDrag = true

            val stepChanged = abs(distanceY).roundToInt() % 3 == 0 && diff > MOVEMENT_TH
            if (!stepChanged || e1.y <= edge || e2.y <= edge) return false

            val onRight = side(e1) ?: return false
            val up = distanceY > 0
            //the brightness on the right and the volume on the left, or the other way round
            if (onRight != isLevelsInverted) callback.onBrightnessStep(if (up) BRIGHTNESS_STEP else -BRIGHTNESS_STEP)
            else callback.onVolumeStep(if (up) VOLUME_STEP else -VOLUME_STEP)
            true
        } else if (isSlideControl) {
            slideOffset = -(scrollFirstTapX - e2.x).toInt() * 100L
            isDrag = true
            isSlide = true

            callback.onSeekSwipe(slideOffset)
            scrollFirstTapX = e1.x.toInt()

            true
        } else false
    }

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        scaleFactor *= scale.scaleFactor
        scaleFactor = max(0.1f, min(scaleFactor, 1.0f))
        callback.onZoom(scaleFactor > 0.5f)
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean = true
    override fun onScaleEnd(detector: ScaleGestureDetector?) = Unit

    private fun onScrollEnd() {
        callback.onSwipeEnd(if (isSlide) slideOffset else null)

        if (isSlide) {
            isSlide = false
            slideOffset = 0L
            scrollFirstTapX = 0
        }
    }

    //true on the right third, false on the left one, null in the middle
    private fun side(e: MotionEvent): Boolean? = when {
        e.x > view.width - view.width / 3 -> true
        e.x < view.width / 3 -> false
        else -> null
    }

    private companion object {
        const val MOVEMENT_TH = 30
        const val BRIGHTNESS_STEP = 5
        const val VOLUME_STEP = 1
        //without the gestures, a tap and the next one count as one this close together, in ms
        const val TAP_INTERVAL = 500
    }
}

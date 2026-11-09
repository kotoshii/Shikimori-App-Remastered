package com.gnoemes.shikimori.presentation.view.common.widget.shikimori

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver

/**
 * Runs [action] once, the first time some part of this view is on screen while it is on a window: the
 * pictures in user text — images, video thumbnails, poster cards — load only then. A text can hold dozens of
 * large GIFs (a tumblr `.gifv` answers 9.5 MB, critique 634144 has 40; BB_CODES.md §5) or hundreds of
 * posters. Inside a cut text or a closed spoiler a view is not on screen, so it waits.
 */
internal fun View.doWhenFirstShown(action: () -> Unit) {
    FirstShownWatcher(this, action)
}

private class FirstShownWatcher(
        private val view: View,
        private val action: () -> Unit
) : View.OnAttachStateChangeListener {

    private val rect = Rect()
    private var done = false
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { check() }
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { check() }

    init {
        view.addOnAttachStateChangeListener(this)
        if (view.isAttachedToWindow) onViewAttachedToWindow(view)
    }

    override fun onViewAttachedToWindow(v: View) {
        view.viewTreeObserver.addOnScrollChangedListener(scrollListener)
        view.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    //still on the window here, so this is the observer the listeners were added to
    override fun onViewDetachedFromWindow(v: View) = stopWatching()

    private fun stopWatching() {
        view.viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        view.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
    }

    private fun check() {
        if (done || view.width == 0 || !view.isShown || !view.getGlobalVisibleRect(rect)) return
        done = true
        //safe while the observer walks its listeners: it walks a copy
        stopWatching()
        view.removeOnAttachStateChangeListener(this)
        action()
    }
}

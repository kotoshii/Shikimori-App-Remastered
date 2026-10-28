package com.gnoemes.shikimori.presentation.presenter.base

import io.reactivex.disposables.Disposable
import io.reactivex.disposables.SerialDisposable

/**
 * One load at a time: [start] cancels the load still running before it starts the next, so the old
 * one's late answer - or its error - can never land on top of the new one (BUGS #52). Made with
 * [BaseNetworkPresenter.loadSlot], which closes it with the screen.
 *
 * Cancelled first, subscribed second. The other way round, the old load would end after the new one
 * had shown its spinner, and hide it. A cancelled load runs none of its end: whatever it showed while
 * running stays until the new load replaces it, or [cancel]'s caller does.
 */
class LoadSlot : Disposable {

    private val current = SerialDisposable()

    fun start(load: () -> Disposable) {
        cancel()
        if (!isDisposed) current.set(load())
    }

    fun cancel() {
        current.set(null)
    }

    override fun dispose() = current.dispose()

    override fun isDisposed(): Boolean = current.isDisposed
}

package com.gnoemes.shikimori.presentation.presenter.base

import android.util.Log
import com.gnoemes.shikimori.entity.app.domain.Constants
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.CompletableSubject
import java.util.concurrent.TimeUnit

/**
 * «Отменить» for a list entry: a status change or a delete waits a few seconds before it is sent, and
 * can be cancelled until then. Each one waits under its own entry's id, so two quick ones never mix
 * (BUGS #20). When the screen closes, the «Отменить» message goes with it, so [sendAll] sends what
 * still waits instead of dropping it. Owned by a screen's presenter - «Списки», and the title page's
 * 🗑 (BUGS #85); everything here runs on the main thread. The message is `View.showUndoMessage`.
 */
class PendingRateChanges(private val delayMillis: Long = Constants.TASK_LONG_DELAY) {

    private class Waiting(val task: Disposable, val sendNow: CompletableSubject)

    private val waiting = mutableMapOf<Long, Waiting>()
    private var closed = false

    /** True from [add] until the change is sent and answered, or cancelled. */
    fun isWaiting(rateId: Long): Boolean = waiting.containsKey(rateId)

    /** Sends [change] after the wait, unless [cancel] comes first. After [sendAll] the callbacks are skipped. */
    fun add(rateId: Long, change: Completable, onSent: () -> Unit, onError: (Throwable) -> Unit) {
        cancel(rateId)
        val sendNow = CompletableSubject.create()
        val task = Completable.timer(delayMillis, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .ambWith(sendNow)
                .andThen(change)
                .subscribe({
                    waiting.remove(rateId)
                    if (!closed) onSent()
                }, {
                    waiting.remove(rateId)
                    if (closed) Log.e(TAG, "a change sent as the list closed failed", it)
                    else onError(it)
                })
        waiting[rateId] = Waiting(task, sendNow)
    }

    fun cancel(rateId: Long) {
        waiting.remove(rateId)?.task?.dispose()
    }

    /** The list is closing: the changes still in their wait are sent now; the ones already sent finish. */
    fun sendAll() {
        closed = true
        waiting.values.toList().forEach { it.sendNow.onComplete() }
    }

    private companion object {
        const val TAG = "PendingRateChanges"
    }
}

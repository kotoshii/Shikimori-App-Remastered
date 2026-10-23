package com.gnoemes.shikimori.utils

import com.gnoemes.shikimori.entity.app.domain.exceptions.HiddenListException
import com.gnoemes.shikimori.entity.app.domain.exceptions.ServiceCodeException
import com.gnoemes.shikimori.utils.rx.ErrorProcessing
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

fun Completable.applyCompleteSchedulers(scheduler: Scheduler = Schedulers.io()): Completable {
    return compose {
        it.subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun <T> Observable<T>.applySchedulers(scheduler: Scheduler = Schedulers.io()): Observable<T> {
    return compose {
        it.subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun <T> Single<T>.applySingleSchedulers(scheduler: Scheduler = Schedulers.io()): Single<T> {
    return compose {
        it.subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
    }
}

fun Completable.applyErrorHandler(processing: ErrorProcessing<*>): Completable {
    return compose { it.onErrorResumeNext(processing::getCompletableErrors) }
}

inline fun <reified T> Observable<T>.applyErrorHandler(processing: ErrorProcessing<T>): Observable<T> {
    return compose { it.onErrorResumeNext(processing::getObservableErrors) }
}

inline fun <reified T> Single<T>.applyErrorHandler(processing: ErrorProcessing<T>): Single<T> {
    return compose { it.onErrorResumeNext(processing::getSingleErrors) }
}

fun Completable.applyErrorHandlerAndSchedulers(scheduler: Scheduler = Schedulers.io()): Completable {
    return applyErrorHandler(ErrorProcessing<Any>()).applyCompleteSchedulers(scheduler)
}

inline fun <reified T> Single<T>.applyErrorHandlerAndSchedulers(scheduler: Scheduler = Schedulers.io()): Single<T> {
    return applyErrorHandler(ErrorProcessing()).applySingleSchedulers(scheduler)
}

inline fun <reified T> Observable<T>.applyErrorHandlerAndSchedulers(scheduler: Scheduler = Schedulers.io()): Observable<T> {
    return applyErrorHandler(ErrorProcessing()).applySchedulers(scheduler)
}

/**
 * A 403 on a user's list or history: its owner's «Могут видеть мой список» hides it from the viewer,
 * signed in or not (the api's `authorize_lists_access`). After the error handler, which makes the 403
 * a [ServiceCodeException].
 */
fun <T> Single<T>.hiddenListOnForbidden(isGuest: () -> Boolean): Single<T> =
        onErrorResumeNext { e: Throwable ->
            Single.error(if ((e as? ServiceCodeException)?.serviceCode == 403) HiddenListException(isGuest()) else e)
        }

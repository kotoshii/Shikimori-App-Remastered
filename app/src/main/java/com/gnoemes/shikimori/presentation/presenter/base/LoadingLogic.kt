package com.gnoemes.shikimori.presentation.presenter.base

import com.gnoemes.shikimori.entity.app.domain.exceptions.NetworkException
import com.gnoemes.shikimori.entity.app.domain.exceptions.ServiceCodeException
import com.gnoemes.shikimori.presentation.view.base.activity.BaseView
import io.reactivex.Single

//what a presenter's request shows while it runs: the full loading state, or the light one

/**
 * The full loading state: the spinner, and the content hidden until the answer. A [refresh] (a
 * pull-to-refresh) keeps the content already shown under the spinner until the answer replaces it: it
 * used to blank the list for the whole load (BUGS #66).
 */
fun <T> Single<T>.appendLoadingLogic(viewState: BaseView, refresh: Boolean = false): Single<T> =
        this.doOnSubscribe { viewState.onShowLoading() }
                .doOnSubscribe { viewState.hideEmptyView() }
                .doOnSubscribe { viewState.hideNetworkView() }
                .doOnSubscribe { if (!refresh) viewState.showContent(false) }
                .doAfterTerminate { viewState.onHideLoading() }
                .doOnEvent { _, _ -> viewState.onHideLoading() }
                .doOnSuccess { viewState.showContent(true) }
                //content was hidden on subscribe, so a failure that nobody recognises used to leave
                //the screen blank for good: it comes back for those. Errors with a view of their own -
                //network and http codes, BaseNetworkPresenter.processErrors - leave it hidden: it came
                //back first and then went again, and an animated list flashed its old items before the
                //error view (P7-T5 15.8). A caller that keeps its content on such an error shows it itself.
                .doOnError { if (it !is NetworkException && it !is ServiceCodeException) viewState.showContent(true) }

fun <T> Single<T>.appendLightLoadingLogic(viewState: BaseView): Single<T> =
        this.doOnSubscribe { viewState.onShowLightLoading() }
                .doAfterTerminate { viewState.onHideLightLoading() }
                .doOnEvent { _, _ -> viewState.onHideLightLoading() }

package com.gnoemes.shikimori.presentation.presenter.common.paginator

interface ViewController<T> {

    fun showEmptyError(show: Boolean, throwable: Throwable? = null)

    fun showEmptyView(show: Boolean)

    fun showData(show: Boolean, data: List<T> = emptyList())

    fun showRefreshProgress(show: Boolean)

    fun showPageProgress(show: Boolean)

    fun showEmptyProgress(show: Boolean) = Unit

    fun showError(throwable: Throwable)

    /** A page after the first failed to load; [throwable] comes with show = true only. */
    fun showPageError(show: Boolean, throwable: Throwable? = null)

    fun onAllData() = Unit
}
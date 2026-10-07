package com.gnoemes.shikimori.presentation.presenter.common.paginator

interface Paginator {
    fun refresh()
    fun loadNewPage()
    fun release()
}
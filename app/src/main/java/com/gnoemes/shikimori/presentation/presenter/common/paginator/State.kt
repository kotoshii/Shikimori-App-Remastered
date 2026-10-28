package com.gnoemes.shikimori.presentation.presenter.common.paginator

import com.gnoemes.shikimori.entity.common.domain.Page

interface State<T> {
    fun refresh() = Unit
    fun loadNewPage() = Unit
    fun release() = Unit
    fun newData(page: Page<T>) = Unit
    fun error(error: Throwable) = Unit
}
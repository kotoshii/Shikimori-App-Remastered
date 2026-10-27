package com.gnoemes.shikimori.presentation.presenter.base

import com.arellomobile.mvp.MvpPresenter
import com.gnoemes.shikimori.presentation.view.base.activity.BaseView
import ru.terrakok.cicerone.Router

abstract class BasePresenter<View : BaseView> : MvpPresenter<View>() {

    private var firstAttach: Boolean = true

    abstract val router: Router

    abstract fun onBackPressed()

    abstract fun initData()

    /** «Повторить» on the screen's error view: its first load again, unless the screen retries otherwise. */
    open fun onRetry() = initData()

    protected open fun onViewReattached() = Unit

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        initData()
    }

    override fun attachView(view: View) {
        super.attachView(view)
        if (!firstAttach) onViewReattached()
        firstAttach = false
    }
}
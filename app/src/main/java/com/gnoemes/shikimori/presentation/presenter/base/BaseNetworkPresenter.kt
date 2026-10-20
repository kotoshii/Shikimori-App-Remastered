package com.gnoemes.shikimori.presentation.presenter.base

import android.util.Log
import com.gnoemes.shikimori.entity.app.domain.exceptions.NetworkException
import com.gnoemes.shikimori.entity.app.domain.exceptions.ServiceCodeException
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.view.base.activity.BaseNetworkView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

abstract class BaseNetworkPresenter<View : BaseNetworkView> : BaseNavigationPresenter<View>() {

    //Dagger fills it in every presenter it builds, after the constructor: the error texts need a
    //Context, and a constructor argument would have to be added to every presenter
    @Inject
    lateinit var commonResources: CommonResourceProvider

    private var compositeDisposable = CompositeDisposable()

    override fun initData() {}

    override fun onDestroy() {
        compositeDisposable.clear()
    }

    fun Disposable.addToDisposables() {
        compositeDisposable.add(this)
    }

    protected open fun processErrors(throwable: Throwable) {
        when (throwable) {
            is NetworkException -> viewState.apply { showNetworkView(); showContent(false); showErrorMessage(commonResources.networkError(throwable.kind)) }
            is ServiceCodeException -> viewState.apply { showNetworkView(); showContent(false); showErrorMessage(commonResources.serverError(throwable.serviceCode)) }
            else -> Log.e("Error", throwable.toString())
        }
    }
}
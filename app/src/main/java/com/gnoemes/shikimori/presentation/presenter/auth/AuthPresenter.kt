package com.gnoemes.shikimori.presentation.presenter.auth

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.repository.series.smotretanime.Anime365TokenSource
import com.gnoemes.shikimori.domain.auth.AuthInteractor
import com.gnoemes.shikimori.entity.auth.AuthType
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.view.auth.AuthView
import javax.inject.Inject

@InjectViewState
class AuthPresenter @Inject constructor(
        private val interactor: AuthInteractor,
        private val anime365TokenSource: Anime365TokenSource
) : BaseNetworkPresenter<AuthView>() {

    var authType: AuthType? = null

    override fun initData() {
        when (authType) {
            AuthType.SIGN_IN -> viewState.onSignIn()
            AuthType.SIGN_UP -> viewState.onSignUp()
            else -> viewState.onAnime365()
        }
    }

    fun onAuthCodeReceived(authCode: String?) {
        interactor.signIn(authCode ?: "")
                .subscribe(this::onSuccess, this::processErrors)
                .addToDisposables()
    }

    private fun onSuccess() {
        viewState.onBackPressed()
    }

    fun onAnime365TokenReceived(token: String) {
        anime365TokenSource.setToken(token)
        viewState.onAnime365SignedIn()
    }
}
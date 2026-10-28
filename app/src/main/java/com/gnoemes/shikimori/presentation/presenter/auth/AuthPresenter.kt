package com.gnoemes.shikimori.presentation.presenter.auth

import android.util.Log
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
                .subscribe(this::onSuccess, this::onSignInFailed)
                .addToDisposables()
    }

    private fun onSuccess() {
        viewState.onBackPressed()
    }

    //what the sign-in saved is undone already (AuthInteractor). Every kind of error says so - only the
    //network ones did, and the screen stayed a spinner, or blank, until Back (BUGS #22)
    private fun onSignInFailed(throwable: Throwable) {
        val message = errorMessage(throwable)
        if (message == null) Log.w(TAG, "sign-in failed", throwable)
        viewState.onSignInFailed(message)
    }

    companion object {
        private const val TAG = "SignIn"
    }

    fun onAnime365TokenReceived(token: String) {
        anime365TokenSource.setToken(token)
        viewState.onAnime365SignedIn()
    }
}
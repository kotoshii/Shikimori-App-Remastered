package com.gnoemes.shikimori.presentation.presenter.main

import android.util.Log
import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.data.repository.app.AppUpdateSource
import com.gnoemes.shikimori.data.repository.common.GenreVocabularySource
import com.gnoemes.shikimori.entity.main.BottomScreens
import com.gnoemes.shikimori.presentation.presenter.base.BaseNavigationPresenter
import com.gnoemes.shikimori.presentation.view.main.MainView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ru.terrakok.cicerone.Router
import javax.inject.Inject

@InjectViewState
class MainPresenter @Inject constructor(
        private val _router: Router,
        private val genreVocabulary: GenreVocabularySource,
        private val appUpdateSource: AppUpdateSource
) : BaseNavigationPresenter<MainView>() {

    private var disposable: CompositeDisposable = CompositeDisposable()

    override val router: Router
        get() = _router

    override fun initData() {
        onTabItemSelected(BottomScreens.RATES)
        refreshGenres()
        checkForUpdate()
    }

    /** Once per start; a newer release shows up as a notification and the settings badge. */
    private fun checkForUpdate() {
        val d = appUpdateSource.check()
                .subscribeOn(Schedulers.io())
                .subscribe({}, { Log.w(TAG, "update check failed", it) })
        disposable.add(d)
    }

    /**
     * One request on start, merged into the stored vocabulary. The filter screen reads that store
     * and never waits for this - a failed refresh simply leaves the previous list in place, which
     * is the whole point of accumulating rather than replacing.
     */
    private fun refreshGenres() {
        val d = genreVocabulary.refresh()
                .subscribeOn(Schedulers.io())
                .subscribe({}, { Log.w(TAG, "genre list refresh failed", it) })
        disposable.add(d)
    }

    //the list and the catalog let their first screen act first (scroll up, open the search); every
    //tab goes back to its first screen otherwise
    fun onTabItemReselected(screenKey: String) {
        if (screenKey == BottomScreens.RATES || screenKey == BottomScreens.SEARCH) viewState.tabRootActionOrClearBackStack(screenKey)
        else viewState.clearBackStack(screenKey)
    }

    //«Ещё» opens on its first screen each time
    fun onTabItemSelected(screenKey: String) {
        if (screenKey == BottomScreens.MORE) viewState.clearBackStack(screenKey)
        router.replaceScreen(screenKey)
    }

    override fun onDestroy() {
        super.onDestroy()

        disposable.clear()
    }

    companion object {
        private const val TAG = "MainPresenter"
    }
}
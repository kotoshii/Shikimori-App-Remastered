package com.gnoemes.shikimori.presentation.presenter.main

import android.util.Log
import com.arellomobile.mvp.InjectViewState
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
        private val genreVocabulary: GenreVocabularySource
) : BaseNavigationPresenter<MainView>() {

    private var disposable: CompositeDisposable = CompositeDisposable()

    override val router: Router
        get() = _router

    override fun initData() {
        onTabItemSelected(BottomScreens.RATES)
        refreshGenres()
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

    fun onTabItemReselected(screenKey: String) {
        when (screenKey) {
            BottomScreens.RATES -> viewState.rateActionOrClearBackStack()
            BottomScreens.CALENDAR -> viewState.clearCalendarBackStack()
            BottomScreens.SEARCH -> viewState.searchActionOrClearSearchBackStack()
            BottomScreens.MAIN -> viewState.clearMainBackStack()
            BottomScreens.MORE -> viewState.clearMoreBackStack()
        }
    }

    fun onTabItemSelected(screenKey: String) {
        when (screenKey) {
            BottomScreens.RATES -> router.replaceScreen(BottomScreens.RATES)
            BottomScreens.CALENDAR -> router.replaceScreen(BottomScreens.CALENDAR)
            BottomScreens.SEARCH -> router.replaceScreen(BottomScreens.SEARCH)
            BottomScreens.MAIN -> router.replaceScreen(BottomScreens.MAIN)
            BottomScreens.MORE -> {
                viewState.clearMoreBackStack()
                router.replaceScreen(BottomScreens.MORE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        disposable.clear()
    }

    companion object {
        private const val TAG = "MainPresenter"
    }
}
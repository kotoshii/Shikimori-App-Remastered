package com.gnoemes.shikimori.presentation.presenter.base

import com.gnoemes.shikimori.entity.common.domain.CommonNavigationData
import com.gnoemes.shikimori.entity.common.domain.Screens
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.presentation.view.base.activity.BaseView
import ru.terrakok.cicerone.Router


abstract class BaseNavigationPresenter<View : BaseView> : BasePresenter<View>() {

    //set by the screen, from its tab's RouterProvider. A screen without one must not navigate: this
    //used to fall back to a new Router() that went nowhere, so such a call failed silently; now it
    //throws at once. Errors are shown by the screen itself (BaseNetworkView.showErrorMessage)
    lateinit var localRouter: Router

    override val router: Router
        get() = localRouter

    override fun onBackPressed() = router.exit()

    open fun onAnimeClicked(id: Long) {
        router.navigateTo(Screens.ANIME_DETAILS, id)
    }

    open fun onMangaClicked(id: Long) {
        router.navigateTo(Screens.MANGA_DETAILS, CommonNavigationData(id, Type.MANGA))
    }

    open fun onRanobeClicked(id: Long){
        router.navigateTo(Screens.MANGA_DETAILS, CommonNavigationData(id, Type.RANOBE))
    }

    open fun onCharacterClicked(id: Long) {
        router.navigateTo(Screens.CHARACTER_DETAILS, id)
    }

    open fun onUserClicked(id: Long) {
        router.navigateTo(Screens.USER_DETAILS, id)
    }

    open fun onPersonClicked(id: Long) {
        router.navigateTo(Screens.PERSON_DETAILS, id)
    }

    open fun onTopicClicked(id: Long){
        router.navigateTo(Screens.TOPIC_DETAILS, id)
    }

    open fun onClubClicked(id: Long){
        router.navigateTo(Screens.CLUB_DETAILS, id)
    }

    open fun onOpenWeb(url: String?) {
        router.navigateTo(Screens.WEB, url)
    }

    fun onContentClicked(type: Type, id: Long) {
        when (type) {
            Type.ANIME -> onAnimeClicked(id)
            Type.MANGA -> onMangaClicked(id)
            Type.RANOBE -> onRanobeClicked(id)
            Type.CHARACTER -> onCharacterClicked(id)
            Type.USER -> onUserClicked(id)
            Type.PERSON -> onPersonClicked(id)
            Type.TOPIC -> onTopicClicked(id)
            Type.CLUB -> onClubClicked(id)
            else -> Unit
        }
    }

}
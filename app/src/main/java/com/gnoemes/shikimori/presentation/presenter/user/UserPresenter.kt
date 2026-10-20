package com.gnoemes.shikimori.presentation.presenter.user

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.user.UserInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.auth.AuthType
import com.gnoemes.shikimori.entity.common.domain.Screens
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.main.BottomScreens
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.presentation.RateNavigationData
import com.gnoemes.shikimori.entity.user.domain.UserDetails
import com.gnoemes.shikimori.entity.user.domain.UserStatus
import com.gnoemes.shikimori.entity.user.presentation.UserContentType
import com.gnoemes.shikimori.entity.user.presentation.UserHistoryNavigationData
import com.gnoemes.shikimori.entity.user.presentation.UserProfileAction
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.presenter.user.converter.UserDetailsViewModelConverter
import com.gnoemes.shikimori.presentation.view.user.UserView
import com.gnoemes.shikimori.utils.appendHostIfNeed
import com.gnoemes.shikimori.presentation.presenter.base.appendLoadingLogic
import io.reactivex.Completable
import io.reactivex.Single
import java.net.URLEncoder
import javax.inject.Inject

@InjectViewState
class UserPresenter @Inject constructor(
        private val interactor: UserInteractor,
        private val converter: UserDetailsViewModelConverter,
        private val resourceProvider: CommonResourceProvider
) : BaseNetworkPresenter<UserView>() {

    var id: Long = Constants.NO_ID

    //the «Профиль» tab: it shows whoever is signed in now, looked up again each time, as the app may
    //have signed out or another account signed in since (BUGS #70)
    private var isMe = false

    private var animeExpanded = false
    private var mangaExpanded = false

    private lateinit var currentUser: UserDetails

    override fun initData() {
        if (id == Constants.NO_ID) {
            isMe = true
            viewState.addSettings()
            showMyProfile(reload = true)
        } else loadData()
    }

    override fun onViewReattached() {
        if (isMe) showMyProfile(reload = false)
    }

    /** The signed-in user's profile, or «Войти»; [reload] loads the profile even when it is the one shown. */
    private fun showMyProfile(reload: Boolean) {
        if (isGuest()) showSignedOut()
        else interactor.getMyUserId()
                .subscribe({
                    if (reload || it != id) {
                        id = it
                        viewState.showAuthView(false)
                        loadData()
                    }
                }, this::processErrors)
                .addToDisposables()
    }

    private fun showSignedOut() {
        id = Constants.NO_ID
        viewState.hideNetworkView()
        viewState.showContent(false)
        viewState.showAuthView(true)
    }

    private fun loadData() =
            loadUser()
                    .doOnSuccess { loadFavorites() }
                    .doOnSuccess { loadFriends() }
                    .doOnSuccess { loadClubs() }
                    .subscribe({}, this::processErrors)
                    .addToDisposables()

    private fun loadUser(showLoading: Boolean = true): Single<UserDetails> =
            interactor.getDetails(id)
                    .flatMap {
                        if (showLoading) Single.just(it).appendLoadingLogic(viewState)
                        else Single.just(it)
                    }
                    .doOnSuccess { currentUser = it }
                    .doOnSuccess { viewState.setInfo(converter.convertInfo(it)) }
                    .doOnSuccess { viewState.setHead(converter.convertHead(it)) }
                    .doOnSuccess { viewState.setAnimeRate(converter.convertAnimeRate(it.stats)) }
                    .doOnSuccess { viewState.setMangaRate(converter.convertMangaRate(it.stats)) }

    private fun loadFavorites() =
            interactor.getFavorites(id)
                    .map { converter.convertFavorites(it) }
                    .subscribe({ viewState.setFavorites(currentUser.isMe, it) }, this::processErrors)
                    .addToDisposables()

    private fun loadFriends() =
            interactor.getFriends(id)
                    .map { converter.convertFriends(it) }
                    .subscribe({ viewState.setFriends(currentUser.isMe, it) }, this::processErrors)
                    .addToDisposables()

    private fun loadClubs() =
            interactor.getClubs(id)
                    .map { converter.convertClubs(it) }
                    .subscribe({ viewState.setClubs(currentUser.isMe, it) }, this::processErrors)
                    .addToDisposables()

    fun onAction(action: UserProfileAction) {
        when (action) {
            is UserProfileAction.History -> onHistoryClicked()
            is UserProfileAction.About -> onAboutClicked()
            is UserProfileAction.Message -> onMessageClicked()
            is UserProfileAction.MessageBox -> onMessageBoxClicked()
            is UserProfileAction.More -> onMoreClicked(action.type)
            is UserProfileAction.ChangeIgnoreStatus -> onIgnoreStatusChanged(action.newStatus)
            is UserProfileAction.ChangeFriendshipStatus -> onFriendshipStatusChanged(action.newStatus)
            is UserProfileAction.RateClicked -> onRateClicked(action.isAnime, action.status)
        }
    }

    fun onArrowClicked(isAnime: Boolean) {
        if (isAnime) {
            animeExpanded = !animeExpanded
            viewState.toggleAnimeRate(animeExpanded)
        } else {
            mangaExpanded = !mangaExpanded
            viewState.toggleMangaRate(mangaExpanded)
        }
    }

    fun onSettingsClicked() {
        router.navigateTo(Screens.SETTINGS)
    }

    private fun onRateClicked(anime: Boolean, status: RateStatus) {
        val data = RateNavigationData(id, if (anime) Type.ANIME else Type.MANGA, status)
        router.navigateTo(BottomScreens.RATES, data)
    }

    private fun onFriendshipStatusChanged(newStatus: Boolean) {
        if (checkUserStatus()) {
            router.showSystemMessage(resourceProvider.needAuth)
            return
        }

        (if (newStatus) interactor.addToFriends(id)
        else interactor.removeFriend(id))
                .updateUserData()
    }

    private fun onIgnoreStatusChanged(newStatus: Boolean) {
        if (checkUserStatus()) return

        (if (newStatus) interactor.ignore(id)
        else interactor.unignore(id))
                .updateUserData()
    }

    private fun onMoreClicked(type: UserContentType) {
        when (type) {
            UserContentType.FRIENDS -> {
                router.navigateTo(Screens.USER_FRIENDS, id)
            }
            UserContentType.CLUBS -> {
                router.navigateTo(Screens.USER_CLUBS, id)
            }
            UserContentType.FAVORITES -> {
                router.navigateTo(Screens.USER_FAVORITES, id)
            }
        }
    }

    private fun onMessageBoxClicked() {
        if (checkUserStatus()) return
        //TODO
    }

    private fun onMessageClicked() {
        if (checkUserStatus()) return
        //TODO
    }

    private fun onAboutClicked() {
        onOpenWeb(URLEncoder.encode(currentUser.nickname, "utf-8").appendHostIfNeed())
    }

    private fun onHistoryClicked() {
        router.navigateTo(Screens.USER_HISTORY, UserHistoryNavigationData(id, currentUser.nickname))
    }

    private fun Completable.updateUserData() {
        andThen(loadUser(false))
                .subscribe({ }, this@UserPresenter::processErrors)
                .addToDisposables()
    }

    fun onRefresh() {
        if (isMe) showMyProfile(reload = true)
        else loadData()
    }

    private fun checkUserStatus(): Boolean {
        return if (isGuest()) {
            router.showSystemMessage(resourceProvider.needAuth)
            true
        } else false
    }

    private fun isGuest(): Boolean = interactor.getUserStatus() == UserStatus.GUEST

    //the tab's own profile shows «Войти» after a sign-out, whatever failed
    override fun processErrors(throwable: Throwable) {
        if (isMe && isGuest()) {
            //once, while the profile is still shown: several requests can fail on one sign-out
            if (id != Constants.NO_ID) showSignedOutMessage(throwable)
            showSignedOut()
        } else super.processErrors(throwable)
    }

    fun onSignIn() = openAuth(AuthType.SIGN_IN)
    fun onSignUp() = openAuth(AuthType.SIGN_UP)

    private fun openAuth(type: AuthType) {
        router.navigateTo(Screens.AUTHORIZATION, type)
    }
}
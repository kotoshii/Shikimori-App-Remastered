package com.gnoemes.shikimori.presentation.presenter.base

import android.util.Log
import com.gnoemes.shikimori.domain.user.UserLookup
import com.gnoemes.shikimori.entity.anime.domain.Screenshot
import com.gnoemes.shikimori.entity.anime.domain.ScreenshotsNavigationData
import com.gnoemes.shikimori.entity.app.domain.exceptions.NetworkException
import com.gnoemes.shikimori.entity.app.domain.exceptions.ServiceCodeException
import com.gnoemes.shikimori.entity.common.domain.Screens
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.richtext.Image
import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.presentation.presenter.common.provider.CommonResourceProvider
import com.gnoemes.shikimori.presentation.view.base.activity.BaseNetworkView
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

abstract class BaseNetworkPresenter<View : BaseNetworkView> : BaseNavigationPresenter<View>(), RichTextListener {

    //Dagger fills it in every presenter it builds, after the constructor: the error texts need a
    //Context, and a constructor argument would have to be added to every presenter
    @Inject
    lateinit var commonResources: CommonResourceProvider

    //the same way, for the links in user text that name a user but carry no id
    @Inject
    lateinit var userLookup: UserLookup

    private var compositeDisposable = CompositeDisposable()

    override fun initData() {}

    /**
     * A tap on a link in user text (P8-T4): a title, a character, a person, a user or a topic opens its
     * screen; a comment opens in a sheet over this screen; a studio opens the catalog filtered by it; any
     * other page opens outside the app, through the navigator, which says so when nothing can open it.
     */
    override fun onLinkClicked(target: LinkTarget) {
        when (target) {
            is LinkTarget.Screen ->
                if (target.type == Type.COMMENT) viewState.showComment(target.id, shownTopicId)
                else onContentClicked(target.type, target.id)
            is LinkTarget.UserByNickname -> userLookup.idByNickname(target.nickname)
                    .subscribe(this::onUserClicked, this::showErrorText)
                    .addToDisposables()
            is LinkTarget.Studio -> onStudioClicked(target.id, target.name)
            is LinkTarget.Web -> onOpenWeb(target.url)
        }
    }

    /** Opens the zoom screen on the images of a text, at the one tapped. */
    override fun onImageClicked(images: List<Image>, index: Int) {
        val items = images.map { Screenshot(it.zoomUrl ?: it.url, it.url) }
        router.navigateTo(Screens.SCREENSHOTS, ScreenshotsNavigationData(index, items, galleryName))
    }

    /** The topic this screen shows: a comment from it opens in the sheet without «Открыть обсуждение». */
    protected open val shownTopicId: Long? get() = null

    /** Names the folder that images enlarged from this screen's texts are saved in: a title, a topic. */
    protected open val galleryName: String get() = ""

    override fun onDestroy() {
        compositeDisposable.clear()
    }

    fun Disposable.addToDisposables() {
        compositeDisposable.add(this)
    }

    /** A [LoadSlot] for the loads that replace each other on this screen; it closes with the screen. */
    protected fun loadSlot(): LoadSlot = LoadSlot().also { it.addToDisposables() }

    protected open fun processErrors(throwable: Throwable) {
        val message = errorMessage(throwable)
        if (message != null) viewState.apply { showNetworkView(); showContent(false); showErrorMessage(message) }
        else Log.e("Error", throwable.toString())
    }

    /** The error as a message only, for a request that does not own the screen; one with no text is logged. */
    protected fun showErrorText(throwable: Throwable) {
        val message = errorMessage(throwable)
        if (message != null) viewState.showErrorMessage(message)
        else Log.e("Error", throwable.toString())
    }

    /**
     * An error while the screen shows its content - a pull-to-refresh, a reload - is a message, and the
     * content stays: a failed refresh hid it all behind the error view (BUGS #78, P7-T5 Q165). One that
     * [takesScreen] replaces the content all the same.
     */
    protected fun showErrorOverContent(throwable: Throwable) {
        if (takesScreen(throwable)) processErrors(throwable) else showErrorText(throwable)
    }

    /** For a load that may be a refresh: [showErrorOverContent] while the content is on screen, else [processErrors]. */
    protected fun loadErrors(overContent: Boolean): (Throwable) -> Unit =
            if (overContent) this::showErrorOverContent else this::processErrors

    /** Whether an error replaces the content on screen rather than being a message over it. */
    protected open fun takesScreen(throwable: Throwable): Boolean = false

    /** What the user reads when a request did not get through or the server refused it; null for any other error. */
    protected fun errorMessage(throwable: Throwable): String? = when (throwable) {
        is NetworkException -> commonResources.networkError(throwable.kind)
        is ServiceCodeException -> commonResources.serverError(throwable.serviceCode)
        else -> null
    }

    /**
     * Says why the user was signed out when [throwable] comes from the request on which the app did it
     * (BUGS #70): for a screen that shows «Войти» instead of its error view.
     */
    protected fun showSignedOutMessage(throwable: Throwable) {
        if ((throwable as? NetworkException)?.kind == NetworkException.Kind.SIGNED_OUT)
            viewState.showErrorMessage(commonResources.networkError(NetworkException.Kind.SIGNED_OUT))
    }
}
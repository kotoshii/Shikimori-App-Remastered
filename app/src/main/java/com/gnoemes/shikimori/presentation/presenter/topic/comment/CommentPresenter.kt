package com.gnoemes.shikimori.presentation.presenter.topic.comment

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.comment.CommentInteractor
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.app.domain.exceptions.ServiceCodeException
import com.gnoemes.shikimori.entity.comment.domain.CommentableType
import com.gnoemes.shikimori.entity.comment.presentation.CommentSheetState
import com.gnoemes.shikimori.entity.comment.presentation.CommentViewModel
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.richtext.LinkTarget
import com.gnoemes.shikimori.presentation.presenter.base.BaseNetworkPresenter
import com.gnoemes.shikimori.presentation.presenter.topic.converter.CommentViewModelConverter
import com.gnoemes.shikimori.presentation.view.topic.comment.CommentView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * The comment sheet's one load. Its taps are not handled here: the sheet hands them to the screen under
 * it, whose router opens pages in the right tab — this presenter has no router.
 */
@InjectViewState
class CommentPresenter @Inject constructor(
        private val interactor: CommentInteractor,
        private val converter: CommentViewModelConverter
) : BaseNetworkPresenter<CommentView>() {

    var id: Long = Constants.NO_ID

    /** The topic the screen under the sheet shows, if any. */
    var topicOnScreen: Long? = null

    private val commentLoad = loadSlot()

    //«Повторить» runs it again (BasePresenter.onRetry)
    override fun initData() = load()

    //the HTML is read off the main thread, as everywhere (P8-T4 plan §3.2)
    private fun load() = commentLoad.start {
        viewState.setState(CommentSheetState.Loading)
        interactor.getComment(id)
                .observeOn(Schedulers.computation())
                .map { converter.convertComment(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ viewState.setState(CommentSheetState.Shown(it, openTarget(it))) }, this::onLoadFailed)
    }

    private fun openTarget(comment: CommentViewModel): LinkTarget.Screen? = when (comment.commentableType) {
        CommentableType.TOPIC -> LinkTarget.Screen(Type.TOPIC, comment.commentableId).takeIf { comment.commentableId != topicOnScreen }
        //a comment on a profile's wall
        CommentableType.USER -> LinkTarget.Screen(Type.USER, comment.commentableId)
    }

    private fun onLoadFailed(throwable: Throwable) {
        if ((throwable as? ServiceCodeException)?.serviceCode == NOT_FOUND) {
            viewState.setState(CommentSheetState.Deleted)
            return
        }
        viewState.setState(CommentSheetState.Failed)
        showErrorText(throwable)
    }

    private companion object {
        const val NOT_FOUND = 404
    }
}

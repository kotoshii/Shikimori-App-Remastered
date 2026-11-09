package com.gnoemes.shikimori.presentation.view.topic.details

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragmentView

interface TopicView : BaseFragmentView {

    /** The whole page in reading order, the post's rows then the comments' (TopicPresenter.showRows); the last is shown again on a rebuilt view. */
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showRows(rows: List<Any>)

    /** Back to the post, for a link to this topic in its own text; a tap, so not replayed. */
    @StateStrategyType(SkipStrategy::class)
    fun scrollToTop()

}

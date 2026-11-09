package com.gnoemes.shikimori.presentation.view.topic.comment

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.comment.presentation.CommentSheetState
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragmentView

interface CommentView : BaseFragmentView {

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun setState(state: CommentSheetState)
}

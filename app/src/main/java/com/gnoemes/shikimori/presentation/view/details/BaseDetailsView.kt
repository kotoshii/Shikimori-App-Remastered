package com.gnoemes.shikimori.presentation.view.details

import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.common.domain.Link
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.*
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.user.presentation.UserStatisticItem
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragmentView

interface BaseDetailsView : BaseFragmentView {

    fun setHeadItem(item : DetailsHeadItem)

    fun setInfoItem(item: DetailsInfoItem)

    fun setDescriptionItem(item : DetailsDescriptionItem)

    fun setContentItem(type: DetailsContentType, item : DetailsContentItem)

    fun setActionItem(item : DetailsActionItem)

    /** The list entry's sheet for this title; it reads the entry itself. */
    @StateStrategyType(SkipStrategy::class)
    fun showRateDialog(title : String, targetId: Long, type: Type)

    @StateStrategyType(SkipStrategy::class)
    fun showStatusDialog(id : Long, name : String, currentStatus: RateStatus?, isAnime : Boolean)

    /** The deleted entry's message, with «Отменить» while the delete waits. */
    @StateStrategyType(SkipStrategy::class)
    fun showRateMessage(message: String, rateId: Long)

    @StateStrategyType(SkipStrategy::class)
    fun showLinks(it: List<Link>)

    @StateStrategyType(SkipStrategy::class)
    fun showStatistic(title : String, scores : List<UserStatisticItem>, rates : List<UserStatisticItem>)
}
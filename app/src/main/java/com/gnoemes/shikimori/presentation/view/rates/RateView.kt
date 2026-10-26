package com.gnoemes.shikimori.presentation.view.rates

import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.RateSort
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.presentation.RateCategory
import com.gnoemes.shikimori.presentation.view.base.fragment.BasePaginationView

interface RateView : BasePaginationView {

    /** The list entry's sheet for title [targetId]; it reads the entry itself. */
    @StateStrategyType(SkipStrategy::class)
    fun showRateDialog(title : String, targetId: Long, type: Type)

    @StateStrategyType(SkipStrategy::class)
    fun showSortDialog(sorts: List<Triple<RateSort, String, Boolean>>)

    @StateStrategyType(SkipStrategy::class)
    fun showStatusDialog(id : Long, name : String, currentStatus: RateStatus, isAnime : Boolean)

    fun setNavigationItems(items: List<RateCategory>)

    fun selectType(type: Type)

    fun selectRateStatus(rateStatus: RateStatus)

    fun showEmptySearchView(it: List<Any>)

    @StateStrategyType(SkipStrategy::class)
    fun showPinLimitMessage()

    /** An entry was just pinned (with how to unpin it) or unpinned. */
    @StateStrategyType(SkipStrategy::class)
    fun showPinChangedMessage(pinned: Boolean)

    fun showEmptyRatesView(show : Boolean, isAnime: Boolean? = null)

    fun showNeedAuthView(show : Boolean)

    /** Whose list it is, on another user's; [nickname] is null on the user's own, which shows no line. */
    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showListOwner(nickname: String?, avatar: String?)

    @StateStrategyType(SkipStrategy::class)
    fun showRateMessage(message: String, rateId: Long)

    @StateStrategyType(SkipStrategy::class)
    fun scrollToTop()
}
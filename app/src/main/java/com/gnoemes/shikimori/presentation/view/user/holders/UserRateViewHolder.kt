package com.gnoemes.shikimori.presentation.view.user.holders

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.LayoutUserProfileRatesBinding
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.user.presentation.UserProfileAction
import com.gnoemes.shikimori.entity.user.presentation.UserRateViewModel
import com.gnoemes.shikimori.presentation.view.user.adapter.UserStatisticItemAdapter
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.widgets.VerticalSpaceItemDecorator

class UserRateViewHolder(
        private val view: View,
        private val isAnime: Boolean,
        private val actionCallback: (UserProfileAction) -> Unit,
        private val toggleCallback: (Boolean) -> Unit
) {

    private val binding = LayoutUserProfileRatesBinding.bind(view)

    private var item: UserRateViewModel? = null
    private val holder: RateProgressViewHolder = RateProgressViewHolder(binding.rateProgressLayout.root, isAnime)
    private val scoreAdapter by lazy { UserStatisticItemAdapter() }
    private val typesAdapter by lazy { UserStatisticItemAdapter() }
    private val ratingsAdapter by lazy { UserStatisticItemAdapter() }

    private val smallMargin by lazy { view.context.dp(16) }
    private val bigMargin by lazy { view.context.dp(32) }

    init {
        val rateTypeText: Int = if (isAnime) R.string.profile_rate_anime else R.string.profile_rate_manga
        binding.rateLabel.setText(rateTypeText)
        binding.menuView.onClick { actionCallback.invoke(UserProfileAction.RateClicked(isAnime, RateStatus.WATCHING)) }
        binding.scoreLayout.headerView.setText(R.string.profile_score)
        binding.typesLayout.headerView.setText(R.string.profile_types)
        binding.ratingsLayout.headerView.setText(R.string.profile_ratings)
        binding.ratingsLayout.root.visibleIf { isAnime }
        binding.arrowBtn.onClick { toggleCallback.invoke(isAnime) }

        binding.scoreLayout.headerView.apply { layoutParams = (layoutParams as ConstraintLayout.LayoutParams).apply { topMargin = context.dp(16) } }
        binding.statisticLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_mini_info, 0)
        binding.statisticLabel.onClick {
            view.context.materialDialog().show {
                message(res = if (isAnime) R.string.profile_anime_hint else R.string.profile_manga_hint)
                positiveButton(R.string.common_ok)
            }
        }

        with(binding.scoreLayout.recyclerView) {
            layoutManager = LinearLayoutManager(view.context)
            addItemDecoration(VerticalSpaceItemDecorator(context.dp(8), true, 0, 0))
            adapter = this@UserRateViewHolder.scoreAdapter
        }

        with(binding.typesLayout.recyclerView) {
            layoutManager = LinearLayoutManager(view.context)
            addItemDecoration(VerticalSpaceItemDecorator(context.dp(8), true, 0, 0))
            adapter = this@UserRateViewHolder.typesAdapter
        }

        with(binding.ratingsLayout.recyclerView) {
            layoutManager = LinearLayoutManager(view.context)
            addItemDecoration(VerticalSpaceItemDecorator(context.dp(8), true, 0, 0))
            adapter = this@UserRateViewHolder.ratingsAdapter
        }
    }

    fun toggle(expanded: Boolean) {
        binding.arrowBtn.animate().rotation(if (expanded) 180f else 0f).start()
        updateVisibility(expanded)
    }

    fun bind(item: UserRateViewModel) {
        //shown again when a reload has rates: a section that was empty once stayed hidden (BUGS #58)
        val hasRates = item.rates.toList().sumBy { it.second } > 0
        view.visibleIf { hasRates }
        if (!hasRates) return
        this.item = item

        holder.bind(item.rates)

        scoreAdapter.bindItems(item.scores)
        typesAdapter.bindItems(item.types)
        ratingsAdapter.bindItems(item.ratings)

        with(view) {
            binding.progressView.root.gone()
            binding.rateProgressLayout.root.visible()
            binding.menuView.visible()
            val scoreSubHeader = "${context.getString(R.string.profile_middle_score)} ${item.averageScore}"
            binding.scoreLayout.subHeaderView.text = scoreSubHeader
            updateVisibility(false)
        }
    }

    //back to the start: its spinner in place of the rates shown, the statistics folded
    fun reset() {
        item = null
        with(view) {
            visible()
            binding.progressView.root.visible()
            binding.rateProgressLayout.root.gone()
            binding.menuView.gone()
            binding.arrowBtn.rotation = 0f
        }
        updateVisibility(false)
    }

    private fun updateVisibility(isExpanded: Boolean) {
        if (isExpanded) TransitionManager.beginDelayedTransition(view as ViewGroup, Fade())
        with(view) {
            binding.divider.layoutParams = (binding.divider.layoutParams as ConstraintLayout.LayoutParams).apply { topMargin = if (isExpanded) bigMargin else smallMargin }
            binding.scoreLayout.root.visibleIf { isExpanded && item?.scores?.isNotEmpty() ?: false }
            binding.typesLayout.root.visibleIf { isExpanded && item?.types?.isNotEmpty() ?: false }
            binding.ratingsLayout.root.visibleIf { isExpanded && item?.ratings?.isNotEmpty() ?: false }
        }
    }
}
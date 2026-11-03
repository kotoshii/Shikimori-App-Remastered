package com.gnoemes.shikimori.presentation.view.user.holders

import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.LinearLayout
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.LayoutRateProgressBinding
import com.gnoemes.shikimori.entity.user.presentation.RateProgressStatus
import com.gnoemes.shikimori.utils.color
import com.gnoemes.shikimori.utils.drawable
import com.gnoemes.shikimori.utils.tint
import com.gnoemes.shikimori.utils.visibleIf

class RateProgressViewHolder(
        val view: View,
        val isAnime: Boolean
) {

    private val binding = LayoutRateProgressBinding.bind(view)

    init {
        val progressColor: Int = view.context.color(R.color.rate_default_dark)
        val completedColor: Int = view.context.color(R.color.rate_watched_dark)
        val droppedColor: Int = view.context.color(R.color.rate_dropped_dark)

        val plannedIcon = view.context.drawable(R.drawable.ic_planned)?.apply { tint(progressColor) }
        val progressIcon = view.context.drawable(R.drawable.ic_play_rate)?.apply { tint(progressColor) }
        val completedIcon = view.context.drawable(R.drawable.ic_check)?.apply { tint(completedColor) }
        val droppedIcon = view.context.drawable(R.drawable.ic_close)?.apply { tint(droppedColor) }

        with(view) {
            binding.plannedCountView.setCompoundDrawablesWithIntrinsicBounds(null, null, plannedIcon, null)
            binding.watchingCountView.setCompoundDrawablesWithIntrinsicBounds(null, null, progressIcon, null)
            binding.watchedCountView.setCompoundDrawablesWithIntrinsicBounds(null, null, completedIcon, null)
            binding.droppedCountView.setCompoundDrawablesWithIntrinsicBounds(null, null, droppedIcon, null)

            binding.firstSection.background = ColorDrawable(progressColor)
            binding.secondSection.background = ColorDrawable(progressColor)
            binding.thirdSection.background = ColorDrawable(completedColor)
            binding.fourthSection.background = ColorDrawable(droppedColor)
        }
    }

    fun bind(rates: Map<RateProgressStatus, Int>) {
        with(view) {
            val plannedCount = rates.getValue(RateProgressStatus.PLANNED)
            binding.plannedCountView.text = "$plannedCount"

            val watchedCount = rates.getValue(RateProgressStatus.COMPLETED)
            binding.watchedCountView.text = "$watchedCount"

            val watchingCount = rates.getValue(RateProgressStatus.IN_PROGRESS)
            binding.watchingCountView.text = "$watchingCount"

            val droppedCount = rates.getValue(RateProgressStatus.DROPPED)
            binding.droppedCountView.text = "$droppedCount"

            val sum = rates.toList().sumBy { it.second }

            if (sum != 0) {
                val percents = rates.mapValues { it.value / sum.toDouble() }

                binding.firstSection.changeWeight(percents.getValue(RateProgressStatus.PLANNED))
                binding.secondSection.changeWeight(percents.getValue(RateProgressStatus.IN_PROGRESS))
                binding.thirdSection.changeWeight(percents.getValue(RateProgressStatus.COMPLETED))
                binding.fourthSection.changeWeight(percents.getValue(RateProgressStatus.DROPPED))
                binding.progressContainer.invalidate()
            }
        }
    }

    private fun View.changeWeight(newWeight: Double) {
        visibleIf { newWeight != 0.0 }
        (parent as? View)?.post { layoutParams = (layoutParams as LinearLayout.LayoutParams).apply { weight = newWeight.toFloat() } }
    }


}
package com.gnoemes.shikimori.presentation.view.common.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.common.domain.SpinnerAction
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.presentation.presenter.common.provider.RatingResourceProvider
import com.gnoemes.shikimori.presentation.presenter.common.provider.RatingResourceProviderImpl
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetDialogFragment
import com.gnoemes.shikimori.utils.*
import kotlinx.android.synthetic.main.fragment_edit_rate.*
import kotlinx.android.synthetic.main.layout_edit_rate_content.*
import kotlinx.android.synthetic.main.layout_edit_rate_progress.*
import kotlinx.android.synthetic.main.layout_edit_rate_status.*
import kotlin.math.roundToInt

class EditRateFragment : BaseBottomSheetDialogFragment() {

    private var callback: RateDialogCallback? = null
    private var isAnime: Boolean = true
    private var rate: UserRate? = null

    override var autoExpand: Boolean
        get() = false
        set(value) {}

    private val ratingResourceProvider: RatingResourceProvider by lazy { RatingResourceProviderImpl(context!!) }

    companion object {
        private const val IS_ANIME_KEY = "IS_ANIME_KEY"
        private const val RATE_KEY = "RATE_KEY"
        fun newInstance(isAnime: Boolean = true, rate: UserRate?) = EditRateFragment()
                .withArgs {
                    putBoolean(IS_ANIME_KEY, isAnime)
                    putParcelable(RATE_KEY, rate)
                }
    }

    override fun getDialogLayout(): Int = R.layout.fragment_edit_rate

    override fun onAttach(context: Context) {
        super.onAttach(context)
        peekHeight = context.dp(400)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getDialogLayout(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.apply {
            isAnime = getBoolean(IS_ANIME_KEY, true)
            rate = getParcelable(RATE_KEY)
        }
        callback = parentFragment as? RateDialogCallback

        with(rateSpinnerView) {
            isAnime = this@EditRateFragment.isAnime
            hasEdit = false
            setRateStatus(rate?.status)
            callback = { action, status ->
                if (action == SpinnerAction.RATE_CHANGE) {
                    rate = rate?.copy(status = status)
                    updateDeleteButton(rateSpinnerView.primaryColor, rateSpinnerView.onPrimaryColor)
                }
            }
            updateDeleteButton(rateSpinnerView.primaryColor, rateSpinnerView.onPrimaryColor)
        }

        acceptBtn.onClick {
            callback?.onUpdateRate(createRate())
            dismiss()
        }

        //TODO remove
        if (rateSpinnerView.primaryColor == android.R.color.transparent) deleteBtn.minHeight = context!!.dp(50)

        deleteBtn.visibleIf { rate != null }
        deleteBtn.onClick { callback?.onDeleteRate(rate?.id ?: Constants.NO_ID); dismiss() }

        val rating = rate?.score?.roundToInt() ?: 0
        ratingBar.rating = rating.div(2f)
        ratingBar.setEmptyDrawable(context!!.drawable(R.drawable.ic_big_star_empty)?.apply { tint(context!!.colorAttr(R.attr.colorOnPrimarySecondary)) })
        ratingBar.setOnRatingChangeListener { _, fl ->
            val newRating = (fl * 2).roundToInt()
            countRating(newRating)
        }
        countRating(rating)

        ratingGroup.setOnClickListener {
            val currentRating = ratingValueView.text.toString().toIntOrNull()
            currentRating?.let {
                val newRating = when (it) {
                    10 -> 0
                    else -> it + 1
                }
                countRating(newRating)
            }
        }

        progressIncrementView.setOnClickListener {
            val newValue = progressView.text?.toString()?.toIntOrNull()?.plus(1) ?: 0
            progressView.setText(newValue.toString())
        }

        progressDecrementView.setOnClickListener {
            var newValue = progressView.text?.toString()?.toIntOrNull()?.minus(1) ?: 0
            if (newValue < 0) newValue = 0
            progressView.setText(newValue.toString())
        }

        commentView.setText(rate?.text)

        if (isAnime) {
            progressLabelView.text = context!!.getString(R.string.profile_rate_watched)
            progressView.setText(rate?.episodes?.toString() ?: "0")

            rewatchesLabel.text = context!!.getString(R.string.profile_rate_rewatched)
            toolbar.setTitle(R.string.rates_title_anime)
        } else {
            progressLabelView.text = context!!.getString(R.string.profile_rate_readed)
            progressView.setText(rate?.chapters?.toString() ?: "0")

            rewatchesLabel.text = context!!.getString(R.string.profile_rate_reread)
            toolbar.setTitle(R.string.rates_title_manga)
        }

        rewatchesView.setText(rate?.rewatches?.toString() ?: "0")

    }

    private fun updateDeleteButton(@ColorRes firstColor: Int, @ColorRes secondColor: Int) {
        deleteBtn.apply {
            setStrokeColorResource(secondColor)
            setIconTintResource(secondColor)
            setBackgroundColor(context.color(firstColor))
        }
    }

    private fun createRate(): UserRate =
            UserRate(
                    id = rate?.id ?: Constants.NO_ID,
                    score = Math.round(ratingBar.rating * 2).toDouble(),
                    status = rate?.status,
                    rewatches = rewatchesView?.text?.toString()?.toIntOrNull(),
                    episodes = if (isAnime) progressView.text?.toString()?.toIntOrNull() else null,
                    chapters = if (isAnime) null else progressView.text?.toString()?.toIntOrNull(),
                    text = commentView.text?.toString()
            )

    private fun countRating(rating: Int) {
        ratingValueView.text = rating.toString()
        ratingDescriptionView.text = ratingResourceProvider.getRatingDescription(rating)
        ratingBar.rating = rating.div(2f)
    }

    interface RateDialogCallback {
        fun onUpdateRate(rate: UserRate) = Unit
        fun onDeleteRate(id: Long) = Unit
    }

}
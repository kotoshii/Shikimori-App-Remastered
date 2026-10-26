package com.gnoemes.shikimori.presentation.view.common.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.forEach
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.presentation.presenter.common.provider.RatingResourceProvider
import com.gnoemes.shikimori.presentation.presenter.common.provider.RatingResourceProviderImpl
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetDialogFragment
import com.gnoemes.shikimori.utils.*
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.fragment_edit_rate.*
import kotlinx.android.synthetic.main.layout_edit_rate_content.*
import kotlinx.android.synthetic.main.layout_edit_rate_progress.*
import kotlinx.android.synthetic.main.layout_edit_rate_status.*
import kotlin.math.roundToInt

/**
 * The list entry's sheet. It edits the entry its caller read from shikimori just before opening it
 * (null for a title not in the list), and ✓ sends only what the user changed: a field sent unchanged
 * would put back a value changed elsewhere since - on the site, or by the watch screen's count
 * (BUGS #83).
 */
class EditRateFragment : BaseBottomSheetDialogFragment() {

    private var callback: RateDialogCallback? = null
    private var isAnime: Boolean = true

    /** The entry as the sheet opened with it, never changed: what [changes] compares with. */
    private val original: UserRate? by lazy { arguments?.getParcelable<UserRate>(RATE_KEY) }

    private var status: RateStatus? = null

    private val ratingResourceProvider: RatingResourceProvider by lazy { RatingResourceProviderImpl(context!!) }

    private val chips by lazy {
        mutableListOf(
                ChipRate(R.id.progressBtn, RateStatus.WATCHING, RateStatus.WATCHING == status),
                ChipRate(R.id.plannedBtn, RateStatus.PLANNED, RateStatus.PLANNED == status),
                ChipRate(R.id.reProgressBtn, RateStatus.REWATCHING, RateStatus.REWATCHING == status),
                ChipRate(R.id.completedBtn, RateStatus.COMPLETED, RateStatus.COMPLETED == status),
                ChipRate(R.id.onHoldBtn, RateStatus.ON_HOLD, RateStatus.ON_HOLD == status),
                ChipRate(R.id.droppedBtn, RateStatus.DROPPED, RateStatus.DROPPED == status)
        )
    }

    companion object {
        private const val IS_ANIME_KEY = "IS_ANIME_KEY"
        private const val RATE_KEY = "RATE_KEY"
        private const val STATUS_KEY = "STATUS_KEY"
        private const val TITLE = "TITLE"
        fun newInstance(title: String, isAnime: Boolean = true, rate: UserRate?) = EditRateFragment()
                .withArgs {
                    putString(TITLE, title)
                    putBoolean(IS_ANIME_KEY, isAnime)
                    putParcelable(RATE_KEY, rate)
                }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        peekHeight = context.dp(210)
    }

    override val useWrapperLayout = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isAnime = arguments?.getBoolean(IS_ANIME_KEY, true) ?: true
        status = if (savedInstanceState != null) savedInstanceState.getSerializable(STATUS_KEY) as? RateStatus
        else original?.status
        callback = parentFragment as? RateDialogCallback

        toolbar.title = arguments?.getString(TITLE)

        val rate = original

        deleteBtn.visibleIf { rate?.id != null }
        deleteBtn.onClick { callback?.onDeleteRate(rate?.id ?: Constants.NO_ID); dismiss() }

        acceptBtn.onClick {
            val changes = changes()
            //nothing to send for an entry that is there already; a new one is added even unchanged
            if (rate?.id == null || !changes.isEmpty()) callback?.onUpdateRate(changes)
            dismiss()
        }

        val rating = rate?.score?.roundToInt() ?: 0
        ratingBar.rating = rating.div(2f)
        ratingBar.setEmptyDrawable(context!!.drawable(R.drawable.ic_big_star_empty)?.apply { tint(context!!.colorAttr(R.attr.colorOnPrimarySecondary)) })
        ratingBar.setFilledDrawable(context!!.drawable(R.drawable.ic_big_star_filled)?.apply { tint(context!!.colorAttr(R.attr.colorSecondary)) })
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
        keepCommentInView()

        if (isAnime) {
            progressLabelView.text = context!!.getString(R.string.profile_rate_watched)
            progressView.setText(rate?.episodes?.toString() ?: "0")

            rewatchesLabel.text = context!!.getString(R.string.profile_rate_rewatched)
        } else {
            progressLabelView.text = context!!.getString(R.string.profile_rate_readed)
            progressView.setText(rate?.chapters?.toString() ?: "0")

            rewatchesLabel.text = context!!.getString(R.string.profile_rate_reread)
        }

        rewatchesView.setText(rate?.rewatches?.toString() ?: "0")

        progressLabel.setText(if (isAnime) R.string.rate_watching else R.string.rate_reading)
        reProgressLabel.setText(if (isAnime) R.string.rate_rewatch_short else R.string.rate_rereading)
        completedLabel.setText(if (isAnime) R.string.rate_completed else R.string.rate_readed)

        progressBtn.onClick { onStatusChanged(it.id) }
        plannedBtn.onClick { onStatusChanged(it.id) }
        reProgressBtn.onClick { onStatusChanged(it.id) }
        completedBtn.onClick { onStatusChanged(it.id) }
        onHoldBtn.onClick { onStatusChanged(it.id) }
        droppedBtn.onClick { onStatusChanged(it.id) }

        val checkedItem = chips.firstOrNull { it.isSelected }
        if (checkedItem != null) (rateInclude as? ConstraintLayout)?.findViewById<MaterialButton>(checkedItem.id)?.isSelected = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //the fields keep their own text; the status lives only here
        outState.putSerializable(STATUS_KEY, status)
    }

    /**
     * The comment is the sheet's last field. While it has the keyboard, the sheet scrolls to its end -
     * when it gets focus, and when the sheet shrinks for the keyboard - so the whole field shows, not
     * only the line with the cursor.
     */
    private fun keepCommentInView() {
        val scrollToEnd = { nestedScroll.post { nestedScroll.smoothScrollTo(0, nestedScroll.getChildAt(0).bottom) } }
        commentView.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) scrollToEnd() }
        nestedScroll.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (commentView.hasFocus() && bottom - top != oldBottom - oldTop) scrollToEnd()
        }
    }

    private fun onStatusChanged(id: Int) {
        (rateInclude as? ConstraintLayout)?.forEach { btn ->
            if (btn is MaterialButton) {
                val item = chips.find { it.id == id }
                btn.isSelected = false
                if (btn.id == id && item != null) {
                    btn.isSelected = true
                    status = item.status
                }
            }
        }
    }

    /**
     * The fields the user changed, the rest null - left out of the request. A new entry is compared
     * with an empty one, so it sends only what was set. An empty number field is not sent, as before;
     * an emptied comment is sent as "".
     */
    private fun changes(): UserRate {
        val before = original ?: UserRate()
        val score = (ratingBar.rating * 2).roundToInt()
        val progress = progressView.text?.toString()?.toIntOrNull()
        val rewatches = rewatchesView.text?.toString()?.toIntOrNull()
        val text = commentView.text?.toString().orEmpty()

        return UserRate(
                id = before.id ?: Constants.NO_ID,
                score = score.takeIf { it != (before.score?.roundToInt() ?: 0) }?.toDouble(),
                status = status.takeIf { it != before.status },
                rewatches = rewatches.takeIf { it != (before.rewatches ?: 0) },
                episodes = if (isAnime) progress.takeIf { it != (before.episodes ?: 0) } else null,
                chapters = if (isAnime) null else progress.takeIf { it != (before.chapters ?: 0) },
                text = text.takeIf { it != before.text.orEmpty() }
        )
    }

    private fun UserRate.isEmpty(): Boolean =
            listOf(score, status, rewatches, episodes, chapters, text).all { it == null }

    private fun countRating(rating: Int) {
        ratingValueView.text = rating.toString()
        ratingDescriptionView.text = ratingResourceProvider.getRatingDescription(rating)
        ratingBar.rating = rating.div(2f)
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getDialogLayout(): Int = R.layout.fragment_edit_rate

    interface RateDialogCallback {
        fun onUpdateRate(rate: UserRate) = Unit
        fun onDeleteRate(id: Long) = Unit
    }

    private data class ChipRate(
            val id: Int,
            val status: RateStatus,
            val isSelected: Boolean
    )
}
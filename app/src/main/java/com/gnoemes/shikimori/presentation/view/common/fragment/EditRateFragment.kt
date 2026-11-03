package com.gnoemes.shikimori.presentation.view.common.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.forEach
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentEditRateBinding
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.presentation.presenter.common.provider.RatingResourceProvider
import com.gnoemes.shikimori.presentation.presenter.common.provider.RatingResourceProviderImpl
import com.gnoemes.shikimori.presentation.presenter.rates.EditRatePresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetInjectionDialogFragment
import com.gnoemes.shikimori.utils.*
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt

/**
 * The list entry's sheet. It opens at once, with placeholders, and reads the entry itself
 * ([EditRatePresenter]) - null for a title not in the list. ✓ sends only what the user changed: a
 * field sent unchanged would put back a value changed elsewhere since - on the site, or by the watch
 * screen's count (BUGS #83).
 */
class EditRateFragment : BaseBottomSheetInjectionDialogFragment<EditRatePresenter, EditRateView>(), EditRateView {

    @InjectPresenter
    lateinit var editRatePresenter: EditRatePresenter

    @ProvidePresenter
    fun providePresenter(): EditRatePresenter = presenterProvider.get().apply {
        targetId = arguments!!.getLong(TARGET_ID_KEY)
        type = arguments!!.getSerializable(TYPE_KEY) as Type
    }

    override val presenter: EditRatePresenter
        get() = editRatePresenter

    private var callback: RateDialogCallback? = null
    private val isAnime: Boolean
        get() = arguments?.getSerializable(TYPE_KEY) == Type.ANIME

    /** The entry as read, never changed by the sheet: what [changes] compares with. */
    private var original: UserRate? = null

    //the fields show the entry: filled once, then they keep the user's edits - through a rotation too,
    //when the entry comes to the sheet again
    private var isFilled = false

    private var status: RateStatus? = null

    private val ratingResourceProvider: RatingResourceProvider by lazy { RatingResourceProviderImpl(context!!) }

    private val chips = listOf(
            ChipRate(R.id.progressBtn, RateStatus.WATCHING),
            ChipRate(R.id.plannedBtn, RateStatus.PLANNED),
            ChipRate(R.id.reProgressBtn, RateStatus.REWATCHING),
            ChipRate(R.id.completedBtn, RateStatus.COMPLETED),
            ChipRate(R.id.onHoldBtn, RateStatus.ON_HOLD),
            ChipRate(R.id.droppedBtn, RateStatus.DROPPED)
    )

    companion object {
        private const val TARGET_ID_KEY = "TARGET_ID_KEY"
        private const val TYPE_KEY = "TYPE_KEY"
        private const val RATE_KEY = "RATE_KEY"
        private const val STATUS_KEY = "STATUS_KEY"
        private const val FILLED_KEY = "FILLED_KEY"
        private const val TITLE = "TITLE"

        /** The sheet for the signed-in user's entry of title [targetId]; a ranobe's [type] is RANOBE. */
        fun newInstance(title: String, targetId: Long, type: Type) = EditRateFragment()
                .withArgs {
                    putString(TITLE, title)
                    putLong(TARGET_ID_KEY, targetId)
                    putSerializable(TYPE_KEY, type)
                }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        peekHeight = context.dp(210)
    }

    override val useWrapperLayout = false

    private val binding by viewBinding(FragmentEditRateBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callback = parentFragment as? RateDialogCallback

        binding.toolbar.title = arguments?.getString(TITLE)

        binding.deleteBtn.onClick { callback?.onDeleteRate(original?.id ?: Constants.NO_ID); dismiss() }

        binding.acceptBtn.onClick {
            val changes = changes()
            //nothing to send for an entry that is there already; a new one is added even unchanged
            if (original?.id == null || !changes.isEmpty()) callback?.onUpdateRate(changes)
            dismiss()
        }

        binding.rateContent.ratingBar.setEmptyDrawable(context!!.drawable(R.drawable.ic_big_star_empty)?.apply { tint(context!!.colorAttr(R.attr.colorOnPrimarySecondary)) })
        binding.rateContent.ratingBar.setFilledDrawable(context!!.drawable(R.drawable.ic_big_star_filled)?.apply { tint(context!!.colorAttr(R.attr.colorSecondary)) })
        binding.rateContent.ratingBar.setOnRatingChangeListener { _, fl ->
            val newRating = (fl * 2).roundToInt()
            countRating(newRating)
        }

        binding.rateContent.ratingGroup.setOnClickListener {
            val currentRating = binding.rateContent.ratingValueView.text.toString().toIntOrNull()
            currentRating?.let {
                val newRating = when (it) {
                    10 -> 0
                    else -> it + 1
                }
                countRating(newRating)
            }
        }

        binding.rateContent.progressInclude.progressIncrementView.setOnClickListener {
            val newValue = binding.rateContent.progressInclude.progressView.text?.toString()?.toIntOrNull()?.plus(1) ?: 0
            binding.rateContent.progressInclude.progressView.setText(newValue.toString())
        }

        binding.rateContent.progressInclude.progressDecrementView.setOnClickListener {
            var newValue = binding.rateContent.progressInclude.progressView.text?.toString()?.toIntOrNull()?.minus(1) ?: 0
            if (newValue < 0) newValue = 0
            binding.rateContent.progressInclude.progressView.setText(newValue.toString())
        }

        keepCommentInView()

        if (isAnime) {
            binding.rateContent.progressInclude.progressLabelView.text = context!!.getString(R.string.profile_rate_watched)
            binding.rateContent.progressInclude.rewatchesLabel.text = context!!.getString(R.string.profile_rate_rewatched)
        } else {
            binding.rateContent.progressInclude.progressLabelView.text = context!!.getString(R.string.profile_rate_readed)
            binding.rateContent.progressInclude.rewatchesLabel.text = context!!.getString(R.string.profile_rate_reread)
        }

        binding.rateContent.rateInclude.progressLabel.setText(if (isAnime) R.string.rate_watching else R.string.rate_reading)
        binding.rateContent.rateInclude.reProgressLabel.setText(if (isAnime) R.string.rate_rewatch_short else R.string.rate_rereading)
        binding.rateContent.rateInclude.completedLabel.setText(if (isAnime) R.string.rate_completed else R.string.rate_readed)

        binding.rateContent.rateInclude.progressBtn.onClick { onStatusChanged(it.id) }
        binding.rateContent.rateInclude.plannedBtn.onClick { onStatusChanged(it.id) }
        binding.rateContent.rateInclude.reProgressBtn.onClick { onStatusChanged(it.id) }
        binding.rateContent.rateInclude.completedBtn.onClick { onStatusChanged(it.id) }
        binding.rateContent.rateInclude.onHoldBtn.onClick { onStatusChanged(it.id) }
        binding.rateContent.rateInclude.droppedBtn.onClick { onStatusChanged(it.id) }

        //rebuilt after a rotation, or after Android closed the app: the fields have restored the
        //user's edits themselves, the status and the entry come from here
        if (savedInstanceState?.getBoolean(FILLED_KEY) == true) {
            original = savedInstanceState.getParcelable(RATE_KEY)
            status = savedInstanceState.getSerializable(STATUS_KEY) as? RateStatus
            isFilled = true
            selectStatusChip()
            showContent()
        } else {
            //nothing to save or delete before the entry is in
            binding.acceptBtn.gone()
            binding.deleteBtn.gone()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(FILLED_KEY, isFilled)
        outState.putParcelable(RATE_KEY, original)
        outState.putSerializable(STATUS_KEY, status)
    }

    override fun showRate(rate: UserRate?) {
        original = rate
        if (!isFilled) fill(rate)
        showContent()
    }

    override fun close() = dismiss()

    private fun fill(rate: UserRate?) {
        isFilled = true
        status = rate?.status
        selectStatusChip()

        val rating = rate?.score?.roundToInt() ?: 0
        binding.rateContent.ratingBar.rating = rating.div(2f)
        countRating(rating)

        binding.rateContent.progressInclude.progressView.setText((if (isAnime) rate?.episodes else rate?.chapters)?.toString() ?: "0")
        binding.rateContent.progressInclude.rewatchesView.setText(rate?.rewatches?.toString() ?: "0")
        binding.rateContent.commentView.setText(rate?.text)
    }

    private fun showContent() {
        binding.ratePlaceholder.root.gone()
        binding.rateContent.root.visible()
        binding.acceptBtn.visible()
        binding.deleteBtn.visibleIf { original?.id != null }
    }

    /**
     * The comment is the sheet's last field. While it has the keyboard, the sheet scrolls to its end -
     * when it gets focus, and when the sheet shrinks for the keyboard - so the whole field shows, not
     * only the line with the cursor.
     */
    private fun keepCommentInView() {
        val nestedScroll = binding.nestedScroll
        val commentView = binding.rateContent.commentView
        val scrollToEnd = { nestedScroll.post { nestedScroll.smoothScrollTo(0, nestedScroll.getChildAt(0).bottom) } }
        commentView.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) scrollToEnd() }
        nestedScroll.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (commentView.hasFocus() && bottom - top != oldBottom - oldTop) scrollToEnd()
        }
    }

    private fun onStatusChanged(id: Int) {
        status = chips.find { it.id == id }?.status ?: return
        selectStatusChip()
    }

    private fun selectStatusChip() {
        val selected = chips.find { it.status == status }?.id
        (binding.rateContent.rateInclude.root as? ConstraintLayout)?.forEach { btn ->
            if (btn is MaterialButton) btn.isSelected = btn.id == selected
        }
    }

    /**
     * The fields the user changed, the rest null - left out of the request. A new entry is compared
     * with an empty one, so it sends only what was set. An empty number field is not sent, as before;
     * an emptied comment is sent as "".
     */
    private fun changes(): UserRate {
        val before = original ?: UserRate()
        val score = (binding.rateContent.ratingBar.rating * 2).roundToInt()
        val progress = binding.rateContent.progressInclude.progressView.text?.toString()?.toIntOrNull()
        val rewatches = binding.rateContent.progressInclude.rewatchesView.text?.toString()?.toIntOrNull()
        val text = binding.rateContent.commentView.text?.toString().orEmpty()

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
        binding.rateContent.ratingValueView.text = rating.toString()
        binding.rateContent.ratingDescriptionView.text = ratingResourceProvider.getRatingDescription(rating)
        binding.rateContent.ratingBar.rating = rating.div(2f)
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
            val status: RateStatus
    )
}

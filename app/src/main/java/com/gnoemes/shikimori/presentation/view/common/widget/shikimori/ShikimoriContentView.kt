package com.gnoemes.shikimori.presentation.view.common.widget.shikimori

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ViewShikimoriCollapsedBinding
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextListener
import com.gnoemes.shikimori.entity.common.presentation.richtext.RichTextState
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.visibleIf
import com.gnoemes.shikimori.utils.widgets.UniqueStateLinearLayout

/**
 * User-written text (a description, a post, a comment), built by [RichTextRenderer]. When [expandable],
 * text taller than 80dp is cut there, with the chevron to open it in full. Whether it is cut is decided
 * once, at its first layout: what grows later — a spoiler the user opens, an image that loads — never cuts
 * a text that was shown whole (the user, 2026-11-09, P8-T4 Q22: a short comment was cut, its opened
 * spoiler hidden, the moment the spoiler opened). While cut and closed, a tap anywhere on it opens it; the
 * links and spoilers inside take taps once it is open.
 *
 * Whether it is cut and open, and the spoilers opened, live in a [RichTextState]: this view's own for
 * [setContent], or one its owner keeps for [setContentState], so a list that reuses this view for another
 * row shows each text again as the user left it (the topic page's comments).
 */
class ShikimoriContentView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val collapsedHeight = (resources.displayMetrics.density * 80).toInt()
    private val chevronHeight = (resources.displayMetrics.density * 32).toInt()

    private val binding = ViewShikimoriCollapsedBinding.inflate(LayoutInflater.from(context), this, true)
    private val content = UniqueStateLinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val clamp = Clamp(context)

    private var imageLoader: ImageLoader? = null
    private var listener: RichTextListener? = null

    private var state: RichTextState? = null

    var expandable: Boolean = true
        set(value) {
            field = value
            //decided again at the next layout
            state?.isCut = null
            clamp.requestLayout()
            updateChevron()
        }

    init {
        var margins = IntArray(4)
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.ShikimoriContentView)
            expandable = ta.getBoolean(R.styleable.ShikimoriContentView_expandable, true)
            val all = ta.getDimension(R.styleable.ShikimoriContentView_content_margin, 0f).toInt()
            margins = if (all != 0) intArrayOf(all, all, all, all) else intArrayOf(
                    ta.getDimension(R.styleable.ShikimoriContentView_content_margin_left, 0f).toInt(),
                    ta.getDimension(R.styleable.ShikimoriContentView_content_margin_top, 0f).toInt(),
                    ta.getDimension(R.styleable.ShikimoriContentView_content_margin_right, 0f).toInt(),
                    ta.getDimension(R.styleable.ShikimoriContentView_content_margin_bottom, 0f).toInt())
            //the layout editor's sample text
            if (isInEditMode) content.addView(TextView(context).apply { text = ta.getText(R.styleable.ShikimoriContentView_shikimori_content) })
            ta.recycle()
        }

        clamp.addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        clamp.id = ViewCompat.generateViewId()
        binding.container.addView(clamp, 0, ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(margins[0], margins[1], margins[2], margins[3])
        })
        ConstraintSet().apply {
            clone(binding.container)
            connect(clamp.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(clamp.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            connect(clamp.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            applyTo(binding.container)
        }
        binding.expandView.onClick { setExpanded(state?.isExpanded != true) }
        clamp.setOnClickListener { setExpanded(true) }
        updateChevron()
    }

    /** Once, by the holder: what loads the images, and who answers the taps (the screen's presenter). */
    fun setup(imageLoader: ImageLoader?, listener: RichTextListener?) {
        this.imageLoader = imageLoader
        this.listener = listener
    }

    /**
     * Builds the views for [text], with what the user opens in it kept by this view. The same text again does
     * nothing, so a reload that brings no change keeps what the user opened (an expanded description stays
     * expanded).
     */
    fun setContent(text: RichText?) {
        if (text == state?.text) return
        setContentState(text?.let { RichTextState(it) })
    }

    /**
     * Builds the views for [state]'s text, opened and cut as [state] says, for an owner that keeps one state
     * per item and reuses this view for others (a list's rows). The same state again does nothing.
     */
    fun setContentState(state: RichTextState?) {
        if (state === this.state) return
        this.state = state
        content.removeAllViews()
        if (state != null && !state.text.isEmpty) RichTextRenderer(context, imageLoader, listener, state).render(content)
        clamp.requestLayout()
        updateChevron()
    }

    private fun setExpanded(expanded: Boolean) {
        state?.isExpanded = expanded
        clamp.requestLayout()
        updateChevron()
    }

    private fun updateChevron() {
        val shown = expandable && state?.isCut == true
        val isExpanded = state?.isExpanded == true
        binding.expandView.visibleIf { shown }
        binding.gradient.visibleIf { shown && !isExpanded }
        binding.expandView.setImageResource(if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down)
        //cut and closed, the whole text is one button that opens it
        clamp.isClickable = shown && !isExpanded
    }

    /** Shows its content whole, or its top 80dp while cut and closed; open, it leaves room for the chevron. */
    private inner class Clamp(context: Context) : FrameLayout(context) {

        //cut and closed, a tap opens the text instead of reaching a link or a spoiler in its top lines
        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = isClickable || super.onInterceptTouchEvent(ev)

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val child = getChildAt(0)
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            val full = child.measuredHeight
            val width = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) child.measuredWidth
            else MeasureSpec.getSize(widthMeasureSpec)
            val current = state
            if (current != null && current.isCut == null && width > 0) {
                val cut = expandable && full > collapsedHeight
                current.isCut = cut
                //not while measuring: the chevron is a view of the same layout pass
                if (cut) post { updateChevron() }
            }
            val height = when {
                current == null || current.isCut != true -> full
                current.isExpanded -> full + chevronHeight
                else -> collapsedHeight
            }
            setMeasuredDimension(width, height)
        }
    }
}

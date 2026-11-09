package com.gnoemes.shikimori.presentation.view.common.widget.shikimori

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.Spanned
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.widgets.ShikimoriLinkMovementMethod

/**
 * One run of user text, styled by hand ([RichTextRenderer]) rather than through `Html.fromHtml`, so it
 * looks the same on every Android version. The text can be selected, unless [passPlainTaps]. Its smileys and avatars
 * ([PictureSpan]) load while it is on a window, and animated ones move only then.
 *
 * The pictures are drawn here, after the text, not by their spans: selectable text is drawn through the
 * text editor's cache, one recording per block of lines, recorded again only when the text or its styling
 * changes (`Editor.drawHardwareAccelerated`; Android 9 and 14 sources) — `invalidate()` does not. A
 * picture drawn by its span stayed as it was when its block was recorded: blank when it loaded later, and
 * an animated one kept an old frame's image, which Glide hands to other GIFs (P8-T4: smileys blank, or
 * playing other smileys). The span keeps the place; [onDraw] draws into it on every redraw. The dashed line
 * under a spoiler's label ([SpoilerLabelSpan]) is drawn here too: Android has no dashed underline.
 */
class ShikimoriTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var imageLoader: ImageLoader? = null

    private var pictures: List<PictureSpan> = emptyList()

    private var labels: List<SpoilerLabelSpan> = emptyList()

    //the site's 1 px dashed border (`b-spoiler.sass`), in px here; dashes and gaps three times its width, about
    //as a browser draws them (inferred, not measured)
    private val dashWidth = resources.displayMetrics.density
    private val dashLength = dashWidth * 3
    private val dashPaint = Paint().apply { strokeWidth = dashWidth }

    //held here: a drawable keeps its callback only weakly. A new frame redraws the view, and onDraw the picture
    private val pictureCallback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) = invalidate()
        override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
            handler?.postAtTime(what, who, `when`)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            handler?.removeCallbacks(what, who)
        }
    }

    init {
        setTextIsSelectable(true)
        //one per view (CODE_CONVENTIONS.md §12); set after the line above, which sets its own
        movementMethod = ShikimoriLinkMovementMethod()
    }

    /**
     * Makes the text not selectable, and lets a tap that hits no link, spoiler or picture reach the view
     * under it: a forum row's post, where that tap opens the topic as a tap on the rest of the row does (P8-T4
     * plan, §1). A text view with a movement method takes every tap otherwise, as a selectable one does.
     */
    fun passPlainTaps() {
        setTextIsSelectable(false)
        movementMethod = ShikimoriLinkMovementMethod()
        //after the line above, which makes the view clickable; ShikimoriLinkMovementMethod reads this
        isClickable = false
        isLongClickable = false
        isFocusable = false
    }

    fun setRichText(text: CharSequence) {
        pictures.forEach { it.clear() }
        setText(text, BufferType.SPANNABLE)
        pictures = (text as? Spanned)?.getSpans(0, text.length, PictureSpan::class.java)?.toList().orEmpty()
        labels = (text as? Spanned)?.getSpans(0, text.length, SpoilerLabelSpan::class.java)?.toList().orEmpty()
        if (isAttachedToWindow) loadPictures()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pictures.isEmpty() && labels.isEmpty()) return
        canvas.save()
        //where the text itself is drawn: the spans gave their places in the text's own coordinates
        canvas.translate(totalPaddingLeft.toFloat() - scrollX, totalPaddingTop.toFloat() - scrollY)
        pictures.forEach { it.drawPicture(canvas) }
        layout?.let { layout -> labels.forEach { drawDashes(canvas, layout, it) } }
        canvas.restore()
    }

    /**
     * A dashed line under [label], on every line it covers, where the site puts its border: at the bottom of the
     * label's letters (its descent), in its colour, the links' colour.
     */
    private fun drawDashes(canvas: Canvas, layout: Layout, label: SpoilerLabelSpan) {
        val spanned = text as? Spanned ?: return
        val start = spanned.getSpanStart(label)
        val end = spanned.getSpanEnd(label)
        if (start < 0 || end <= start) return
        dashPaint.color = linkTextColors?.let { it.getColorForState(drawableState, it.defaultColor) } ?: currentTextColor
        val below = paint.descent() * label.scale + dashWidth / 2
        val first = layout.getLineForOffset(start)
        val last = layout.getLineForOffset(end - 1)
        for (line in first..last) {
            val from = if (line == first) layout.getPrimaryHorizontal(start) else layout.getLineLeft(line)
            //the end of a line that wraps inside the label is that line's end, not the next line's start
            val to = if (layout.getLineForOffset(end) == line) layout.getPrimaryHorizontal(end) else layout.getLineRight(line)
            val y = layout.getLineBaseline(line) + below
            var x = minOf(from, to)
            val stop = maxOf(from, to)
            while (x < stop) {
                canvas.drawLine(x, y, minOf(x + dashLength, stop), y, dashPaint)
                x += dashLength * 2
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadPictures()
        pictures.forEach { it.start() }
    }

    override fun onDetachedFromWindow() {
        pictures.forEach { it.stop() }
        super.onDetachedFromWindow()
    }

    private fun loadPictures() {
        val loader = imageLoader ?: return
        for (picture in pictures) {
            if (picture.isLoaded || picture.request != null) continue
            picture.request = loader.loadTextPicture(this, picture.url, picture.size) { drawable ->
                picture.setDrawable(drawable, pictureCallback)
                invalidate()
            }
        }
    }
}

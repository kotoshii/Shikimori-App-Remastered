package com.gnoemes.shikimori.presentation.view.common.widget.shikimori

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ReplacementSpan
import android.view.View
import androidx.annotation.ColorInt
import com.gnoemes.shikimori.utils.images.ImageRequest

/*
 * The parts of user text a tap acts on, and the pictures drawn inside a line. Each tappable span runs its
 * own action; ShikimoriLinkMovementMethod only finds the span under the finger (CODE_CONVENTIONS.md §12).
 */

/** A link: in the theme's link colour, unless the author coloured it; no underline, as on the site. */
class LinkSpan(
        private val keepColor: Boolean,
        private val action: () -> Unit
) : ClickableSpan() {

    override fun onClick(widget: View) = action()

    override fun updateDrawState(ds: TextPaint) {
        if (!keepColor) ds.color = ds.linkColor
        ds.isUnderlineText = false
    }
}

/**
 * Text inside a closed `||spoiler||`: a solid bar of the text's own size. It wins over a link inside it
 * (ShikimoriLinkMovementMethod): the first tap opens the spoiler, as on the site.
 */
class HiddenSpoilerSpan(
        @ColorInt private val barColor: Int,
        private val action: () -> Unit
) : ClickableSpan() {

    override fun onClick(widget: View) = action()

    override fun updateDrawState(ds: TextPaint) {
        ds.bgColor = barColor
        ds.color = barColor
        ds.isUnderlineText = false
    }
}

/**
 * Text inside an open `||spoiler||`: a faint tint of the bar's colour shows how far it reaches, and a tap
 * closes it again, as on the site (`b-spoiler_inline.sass`, `spoiler_inline.js`). A link inside wins.
 */
class OpenSpoilerSpan(
        @ColorInt private val tintColor: Int,
        private val action: () -> Unit
) : ClickableSpan() {

    override fun onClick(widget: View) = action()

    override fun updateDrawState(ds: TextPaint) {
        ds.bgColor = tintColor
    }
}

/**
 * The label of a closed `[spoiler=label]` inside a sentence, or the small mark before an open one's
 * content, which closes it: underlined, in the link colour.
 */
class SpoilerLabelSpan(private val action: () -> Unit) : ClickableSpan() {

    override fun onClick(widget: View) = action()

    override fun updateDrawState(ds: TextPaint) {
        ds.color = ds.linkColor
        ds.isUnderlineText = true
    }
}

/**
 * A picture inside a line — a smiley, an avatar — [size] pixels square, centred on the text as the site
 * does (`vertical-align: middle`). The span keeps its place in the line; the text view loads the picture and
 * draws it there ([ShikimoriTextView.onDraw] says why not here). Inside a closed spoiler it is part of the bar,
 * drawn here: that never changes.
 */
class PictureSpan(
        val url: String,
        val size: Int,
        private val hidden: Boolean,
        @ColorInt private val barColor: Int,
        /** Room after the picture: an avatar keeps a little space before the nickname it stands beside. */
        private val spaceAfter: Int = 0
) : ReplacementSpan() {

    var drawable: Drawable? = null
        private set

    var request: ImageRequest? = null

    val isLoaded: Boolean get() = drawable != null

    /** [callback] redraws the text view: an animated GIF asks for each frame through it. */
    fun setDrawable(drawable: Drawable, callback: Drawable.Callback) {
        drawable.setBounds(0, 0, size, size)
        drawable.callback = callback
        this.drawable = drawable
        start()
    }

    fun start() {
        (drawable as? Animatable)?.takeIf { !it.isRunning }?.start()
    }

    fun stop() {
        (drawable as? Animatable)?.takeIf { it.isRunning }?.stop()
    }

    fun clear() {
        stop()
        request?.cancel()
        request = null
        drawable?.callback = null
        drawable = null
    }

    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        if (fm != null) {
            val text = paint.fontMetricsInt
            val top = (text.ascent + text.descent) / 2 - size / 2
            val bottom = top + size
            fm.ascent = minOf(text.ascent, top)
            fm.top = minOf(text.top, top)
            fm.descent = maxOf(text.descent, bottom)
            fm.bottom = maxOf(text.bottom, bottom)
        }
        return size + 2 * GAP + spaceAfter
    }

    //where the text last placed the picture, in the text's own coordinates; NaN until it is placed
    private var left = Float.NaN
    private var top = Float.NaN

    /** Notes the picture's place; draws only the closed spoiler's bar (see the class comment). */
    override fun draw(canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val metrics = paint.fontMetricsInt
        this.left = x + GAP
        this.top = (y + (metrics.ascent + metrics.descent) / 2 - size / 2).toFloat()
        if (hidden) {
            val color = paint.color
            paint.color = barColor
            canvas.drawRect(x, top.toFloat(), x + size + 2 * GAP + spaceAfter, bottom.toFloat(), paint)
            paint.color = color
        }
    }

    /** By the text view, on every redraw, into the place the text gave. */
    fun drawPicture(canvas: Canvas) {
        val picture = drawable ?: return
        if (hidden || left.isNaN()) return
        canvas.save()
        canvas.translate(left, top)
        picture.draw(canvas)
        canvas.restore()
    }

    private companion object {
        //a sliver of space on each side, so two smileys in a row do not touch
        const val GAP = 2
    }
}

package com.gnoemes.shikimori.presentation.view.common.widget.shikimori

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.widgets.ShikimoriLinkMovementMethod

/**
 * One run of user text, styled by hand ([RichTextRenderer]) rather than through `Html.fromHtml`, so it
 * looks the same on every Android version. The text can be selected. Its smileys and avatars
 * ([PictureSpan]) load while it is on a window, and animated ones move only then.
 *
 * The pictures are drawn here, after the text, not by their spans: selectable text is drawn through the
 * text editor's cache, one recording per block of lines, recorded again only when the text or its styling
 * changes (`Editor.drawHardwareAccelerated`; Android 9 and 14 sources) — `invalidate()` does not. A
 * picture drawn by its span stayed as it was when its block was recorded: blank when it loaded later, and
 * an animated one kept an old frame's image, which Glide hands to other GIFs (P8-T4: smileys blank, or
 * playing other smileys). The span keeps the place; [onDraw] draws into it on every redraw.
 */
class ShikimoriTextView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var imageLoader: ImageLoader? = null

    private var pictures: List<PictureSpan> = emptyList()

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

    fun setRichText(text: CharSequence) {
        pictures.forEach { it.clear() }
        setText(text, BufferType.SPANNABLE)
        pictures = (text as? Spanned)?.getSpans(0, text.length, PictureSpan::class.java)?.toList().orEmpty()
        if (isAttachedToWindow) loadPictures()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (pictures.isEmpty()) return
        canvas.save()
        //where the text itself is drawn: the spans gave their places in the text's own coordinates
        canvas.translate(totalPaddingLeft.toFloat() - scrollX, totalPaddingTop.toFloat() - scrollY)
        pictures.forEach { it.drawPicture(canvas) }
        canvas.restore()
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

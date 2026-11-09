package com.gnoemes.shikimori.presentation.view.common.widget.shikimori

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.widget.AppCompatImageView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.presentation.richtext.Image
import com.gnoemes.shikimori.utils.colorAttr
import com.gnoemes.shikimori.utils.images.ImageLoader

/**
 * An image in user text. It loads only once some part of it is on screen ([doWhenFirstShown] says why).
 *
 * Its size, as the site shows it: an image a tap enlarges fits in a 235 px box (`$media_width`,
 * `b-image.sass`); any other image keeps its own size up to the text's width. The site's pixels are taken
 * as dp. The space is kept from the size the HTML gives (the author's, or an upload's real one) before the
 * image loads, so the text does not jump; without one, a placeholder until it loads.
 */
@SuppressLint("ViewConstructor")
class RichTextImageView(
        context: Context,
        private val image: Image,
        private val imageLoader: ImageLoader?
) : AppCompatImageView(context) {

    private val density = resources.displayMetrics.density
    private val box = if (image.zoomUrl != null && !image.isPoster) (MEDIA_WIDTH * density).toInt() else Int.MAX_VALUE

    //the picture's own size in the site's pixels, once loaded
    private var loadedWidth = 0
    private var loadedHeight = 0
    private var failed = false

    init {
        scaleType = ScaleType.FIT_CENTER
        adjustViewBounds = false
        setBackgroundColor(context.colorAttr(R.attr.colorTextBlockBackground))
        doWhenFirstShown { load() }
    }

    private fun load() {
        val loader = imageLoader ?: return
        //no picture to load (a video with no thumbnail, a censored poster): the placeholder stays
        if (image.url.isBlank()) return
        val maxWidth = minOf(box, maxOf(width, (parent as? android.view.View)?.width ?: width))
        loader.setTextImage(this, image.url, maxWidth, minOf(box, MAX_SIDE), { width, height ->
            loadedWidth = width
            loadedHeight = height
            setBackgroundColor(0)
            requestLayout()
        }, {
            failed = true
            setImageResource(R.drawable.ic_image_unavailable)
            scaleType = ScaleType.CENTER
            requestLayout()
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) box
        else MeasureSpec.getSize(widthMeasureSpec)
        val maxWidth = minOf(available, box)
        val maxHeight = box

        val (naturalWidth, naturalHeight) = naturalSize()
        if (failed && naturalWidth == 0) {
            val side = (FAILED_SIDE * density).toInt()
            setMeasuredDimension(minOf(side, maxWidth), side)
            return
        }
        if (naturalWidth <= 0 || naturalHeight <= 0) {
            setMeasuredDimension(minOf(maxWidth, (MEDIA_WIDTH * density).toInt()), (PLACEHOLDER_HEIGHT * density).toInt())
            return
        }
        //the author's or the picture's size, made smaller to fit, never larger
        val scale = minOf(1f, maxWidth.toFloat() / naturalWidth, maxHeight.toFloat() / naturalHeight)
        setMeasuredDimension(maxOf(1, (naturalWidth * scale).toInt()), maxOf(1, (naturalHeight * scale).toInt()))
    }

    /** In pixels on screen; 0 when not known yet. */
    private fun naturalSize(): Pair<Int, Int> {
        val ratio = when {
            loadedWidth > 0 && loadedHeight > 0 -> loadedHeight.toFloat() / loadedWidth
            image.realWidth != null && image.realHeight != null -> image.realHeight.toFloat() / image.realWidth
            else -> null
        }
        val width = image.width ?: image.height?.let { height -> ratio?.let { (height / it).toInt() } }
                ?: loadedWidth.takeIf { it > 0 } ?: image.realWidth
        val height = image.height ?: width?.let { w -> ratio?.let { (w * it).toInt() } }
        if (width == null || height == null) return 0 to 0
        return (width * density).toInt() to (height * density).toInt()
    }

    private companion object {
        //the site's box for an image a tap enlarges (`$media_width`)
        const val MEDIA_WIDTH = 235
        const val PLACEHOLDER_HEIGHT = 120
        const val FAILED_SIDE = 48
        //a long strip is loaded no taller than this, in pixels
        const val MAX_SIDE = 4096
    }
}

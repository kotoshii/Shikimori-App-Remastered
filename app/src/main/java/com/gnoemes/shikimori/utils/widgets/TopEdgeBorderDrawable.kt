package com.gnoemes.shikimori.utils.widgets

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

/**
 * A line along the top edge and the rounded top corners only: a bottom sheet's border on Amoled, where
 * the sheet is as black as the screen behind it (BUGS #17). A shape's stroke would also run down both
 * sides, and a layer-list item's height that could cut it off needs Android 6.
 */
class TopEdgeBorderDrawable(
        @ColorInt color: Int,
        private val width: Float,
        private val radius: Float
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        this.color = color
    }
    private val path = Path()

    //the line's middle runs half its width inside the bounds, so all of it shows
    override fun onBoundsChange(bounds: Rect) {
        val half = width / 2
        val left = bounds.left + half
        val right = bounds.right - half
        val top = bounds.top + half
        val inner = radius - half

        path.reset()
        path.moveTo(left, top + inner)
        path.arcTo(RectF(left, top, left + 2 * inner, top + 2 * inner), 180f, 90f)
        path.lineTo(right - inner, top)
        path.arcTo(RectF(right - 2 * inner, top, right, top + 2 * inner), 270f, 90f)
    }

    override fun draw(canvas: Canvas) = canvas.drawPath(path, paint)

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

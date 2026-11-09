package com.gnoemes.shikimori.presentation.view.common.widget.shikimori

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.gnoemes.shikimori.utils.dp

/**
 * Poster cards in user text (`[animes ids=…]`, a collection's titles), in equal columns: as many as fit at
 * [MIN_COLUMN_DP] each, two at least, so a phone shows three and a tablet more. A row is as tall as its tallest
 * card. The site sets its own number of columns (`cc-4`) for a desktop's width, which a phone cannot follow.
 */
internal class PosterGridLayout(context: Context) : ViewGroup(context) {

    private val gap = context.dp(GAP_DP)
    private val minColumn = context.dp(MIN_COLUMN_DP)

    private var columns = 1
    private var columnWidth = 0
    private var rowHeights = IntArray(0)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) 2 * minColumn + gap
        else MeasureSpec.getSize(widthMeasureSpec)
        columns = maxOf(2, (width + gap) / (minColumn + gap))
        columnWidth = maxOf(1, (width - gap * (columns - 1)) / columns)
        val rows = (childCount + columns - 1) / columns
        rowHeights = IntArray(rows)
        val childWidth = MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY)
        val childHeight = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            child.measure(childWidth, childHeight)
            val row = index / columns
            rowHeights[row] = maxOf(rowHeights[row], child.measuredHeight)
        }
        val height = rowHeights.sum() + gap * maxOf(0, rows - 1)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var top = 0
        for (row in rowHeights.indices) {
            for (column in 0 until columns) {
                val index = row * columns + column
                if (index >= childCount) break
                val child = getChildAt(index)
                //from the end on a right-to-left screen, as a row of text
                val position = if (layoutDirection == LAYOUT_DIRECTION_RTL) columns - 1 - column else column
                val left = position * (columnWidth + gap)
                child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
            }
            top += rowHeights[row] + gap
        }
    }

    /** A frame of a poster's size: the column's width up to [maxWidthPx]'s dp, and the poster's proportions. */
    @SuppressLint("ViewConstructor")
    class PosterSizedFrame(context: Context, maxWidthPx: Int, heightPx: Int) : FrameLayout(context) {

        private val ratio = heightPx.toFloat() / maxWidthPx
        private val maxWidth = context.dp(maxWidthPx)

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val available = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) maxWidth
            else MeasureSpec.getSize(widthMeasureSpec)
            val width = minOf(available, maxWidth)
            val height = (width * ratio).toInt()
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        }
    }

    private companion object {
        const val GAP_DP = 8
        const val MIN_COLUMN_DP = 96
    }
}

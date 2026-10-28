package com.gnoemes.shikimori.utils.widgets

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GridItemDecorator(
        private val spacing: Int,
        private val margin: Int = spacing
) : RecyclerView.ItemDecoration() {

    private val halfSpacing = spacing / 2

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val columns = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 1
        //the last row's index: itemCount / columns was one past it when the items fill that row, so it
        //got half the gap at the bottom (T5-32)
        val lastRow = ((parent.adapter?.itemCount ?: 1) - 1).coerceAtLeast(0) / columns

        val pos = parent.getChildAdapterPosition(view)

        fun setTopSpacing() {
            if (pos / columns + 1 == 1) {
                outRect.top = margin
            } else {
                outRect.top = halfSpacing
            }
        }

        fun setBottomSpacing() {
            if (pos / columns == lastRow) {
                outRect.bottom = margin
            } else {
                outRect.bottom = halfSpacing
            }
        }

        fun setLeftSpacing() {
            if (pos % columns == 0) {
                outRect.left = margin
            } else {
                outRect.left = halfSpacing
            }
        }

        fun setRightSpacing() {
            if (pos % columns == columns - 1) {
                outRect.right = margin
            } else {
                outRect.right = halfSpacing
            }
        }

        setTopSpacing()
        setBottomSpacing()
        setLeftSpacing()
        setRightSpacing()
    }
}
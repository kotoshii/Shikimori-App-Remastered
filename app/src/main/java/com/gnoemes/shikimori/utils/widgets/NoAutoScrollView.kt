package com.gnoemes.shikimori.utils.widgets

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView

/**
 * A `NestedScrollView` that does not move by itself on a touch screen: it never scrolls to bring a focused
 * view or a cursor into view. The screens with user text use it (the topic page, the comment sheet, the
 * title, character and person pages). A tap on selectable text focuses it, and every later layout of a list
 * inside — a comment opened, a spoiler, an image loaded — made the page jump back to that text: a
 * `RecyclerView` re-attaches its rows and asks for the focus again (`ViewGroup.attachViewToParent` →
 * `requestChildFocus`), and the scroll view scrolls to it (P8-T4, 2026-11-09; CODE_CONVENTIONS.md §12).
 * With a keyboard or a remote (not touch mode) it scrolls to the focus as usual.
 */
class NoAutoScrollView : NestedScrollView {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    //the one place every "bring this into view" of the scroll view goes through: focus, a child's rectangle,
    //a focused view after a size change
    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int =
            if (isInTouchMode) 0 else super.computeScrollDeltaToGetChildRectOnScreen(rect)
}

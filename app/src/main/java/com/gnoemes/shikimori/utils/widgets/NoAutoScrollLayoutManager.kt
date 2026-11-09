package com.gnoemes.shikimori.utils.widgets

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A vertical list that does not move by itself on a touch screen: it never scrolls to bring a focused view or
 * a cursor into view. [NoAutoScrollView]'s rule, for a page that is one list (the topic page). A tap on
 * selectable text focuses it and leaves an invisible cursor; the list would scroll to show the whole focused
 * text (a tap on a comment half off screen), and to bring that cursor into view after a later layout
 * (`TextView.bringPointIntoView` → `requestRectangleOnScreen`). Both come here: `RecyclerView.requestChildFocus`
 * → `requestChildOnScreen`, and `RecyclerView.requestChildRectangleOnScreen` (RecyclerView 1.1.0 sources).
 * With a keyboard or a remote (not touch mode) it scrolls as usual. P8-T4, 2026-11-09; CODE_CONVENTIONS.md §12.
 */
class NoAutoScrollLayoutManager(context: Context) : LinearLayoutManager(context) {

    //the four-argument form calls this one
    override fun requestChildRectangleOnScreen(parent: RecyclerView, child: View, rect: Rect, immediate: Boolean,
                                               focusedChildVisible: Boolean): Boolean =
            if (parent.isInTouchMode) false
            else super.requestChildRectangleOnScreen(parent, child, rect, immediate, focusedChildVisible)
}

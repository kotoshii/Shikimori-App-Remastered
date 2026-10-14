package com.gnoemes.shikimori.presentation.view.rates.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.PaintDrawable
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.entity.rates.domain.RateSwipeAction
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.widgets.ItemTouchHelperAdapter

class RateItemTouchHelperCallback(
        private val adapter: ItemTouchHelperAdapter,
        private val settingsSource: SettingsSource
) : ItemTouchHelper.Callback() {

    private val swipeToLeftAction by lazy { settingsSource.rateSwipeToLeftAction }
    private val swipeToRightAction by lazy { settingsSource.rateSwipeToRightAction }

    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = if (adapter.canDragItem(viewHolder.adapterPosition)) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0
        val swipeFlags = getSwipeFlags(viewHolder)
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    private fun getSwipeFlags(viewHolder: RecyclerView.ViewHolder): Int {
        return if (adapter.canSwipeItem(viewHolder.adapterPosition)) {
            var flags = 0
            if (swipeToLeftAction != RateSwipeAction.DISABLED) flags = flags or ItemTouchHelper.START
            if (swipeToRightAction != RateSwipeAction.DISABLED) flags = flags or ItemTouchHelper.END
            return flags
        } else 0
    }

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        return adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val action = if (direction == ItemTouchHelper.START) swipeToLeftAction else swipeToRightAction
        adapter.onItemSwipeAction(viewHolder.adapterPosition, action)
    }

    override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
        adapter.onSelectedItemMove(toPos)
        super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
    }

    override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val toRight = dX > 0
        drawSwipe(c, recyclerView.context, viewHolder.itemView, if (toRight) swipeToRightAction else swipeToLeftAction,
                dX.toInt(), toRight, isCurrentlyActive)
    }

    /** The coloured background behind a swiped entry, and the action's icon on the side it uncovers. */
    private fun drawSwipe(c: Canvas, context: Context, item: View, action: RateSwipeAction, dX: Int, toRight: Boolean, isCurrentlyActive: Boolean) {
        val look = SwipeLook.of(context, action)

        val background = PaintDrawable(look.background)
        val corners = context.dp(8)
        background.setCornerRadius(corners.toFloat())
        if (toRight) background.setBounds(item.left, item.top, item.left + dX + corners, item.bottom)
        else background.setBounds(item.right + dX - if (isCurrentlyActive) corners else 0, item.top, item.right, item.bottom)
        background.draw(c)

        val icon = look.icon?.let { context.drawable(it) } ?: return
        val margin = context.dp(24)
        val top = item.top + (item.bottom - item.top) / 2 - icon.intrinsicHeight / 2
        val left = if (toRight) item.left + margin else item.right - margin - icon.intrinsicWidth
        icon.setBounds(left, top, left + icon.intrinsicWidth, top + icon.intrinsicHeight)
        icon.tint(look.iconColor)
        icon.draw(c)
    }

    /** What each swipe action looks like: the background colour, the icon and its colour. */
    private class SwipeLook(val background: Int, val iconColor: Int, val icon: Int?) {
        companion object {
            fun of(context: Context, action: RateSwipeAction): SwipeLook = when (action) {
                RateSwipeAction.INCREMENT -> SwipeLook(context.colorAttr(R.attr.colorSecondaryTransparent), context.colorAttr(R.attr.colorSecondary), R.drawable.ic_plus_one_filled)
                RateSwipeAction.CHANGE -> SwipeLook(context.colorAttr(R.attr.colorSecondaryTransparent), context.colorAttr(R.attr.colorSecondary), R.drawable.ic_list_change)
                RateSwipeAction.ON_HOLD -> SwipeLook(context.colorAttr(R.attr.colorRateOnHold), context.color(R.color.rate_on_hold_dark), R.drawable.ic_pause_rate)
                RateSwipeAction.DROP -> SwipeLook(context.colorAttr(R.attr.colorRateDropped), context.color(R.color.rate_dropped_dark), R.drawable.ic_close)
                RateSwipeAction.DISABLED -> SwipeLook(0, 0, null)
            }
        }
    }
}
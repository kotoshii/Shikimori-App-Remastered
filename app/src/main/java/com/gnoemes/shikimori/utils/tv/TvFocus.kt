package com.gnoemes.shikimori.utils.tv

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.utils.colorAttr
import com.gnoemes.shikimori.utils.dp
import com.gnoemes.shikimori.utils.isTvUi

//The TV mode's visible focus. With no pointer, the user only knows where they are from the visuals
//of the view under focus, so every focusable element gets three of them, all tied to the ascent
//color: the accent-tinted ripple (TvFocusOverlay, applied by Activity.setTheme), the outline around
//the view, and the grow-and-lift animation. In the mobile mode nothing of this happens - touches
//never move focus, and a phone screen does not need it.

/** How much a focused view grows, relative to itself. */
private const val FOCUS_SCALE = 1.08f

/** How long the grow/lift takes. */
private const val FOCUS_ANIMATION_MILLIS = 150L

/** The focused view rises above its neighbors, so rows read as a stack. */
private const val FOCUS_LIFT_DP = 8

/** The glow: the accent outline hugging the view's edge. */
private const val BORDER_GLOW_DP = 6

/** The core: a bright line between the glow and the content. The accent alone reads only on light
 * posters - dark ones swallow it - while the bright core stays visible on every background. */
private const val BORDER_CORE_DP = 3

/** How far the bright core sits inside the glow, so the two lines do not overlap. */
private const val BORDER_CORE_INSET_DP = 3

private const val BORDER_RADIUS_DP = 8
private const val BORDER_CORE_ALPHA = 230

/**
 * Marks the view as a D-pad target of the TV mode and gives it the grow, the lift and the outline
 * on focus. Does nothing in the mobile mode, so wired views stay exactly as they were there.
 *
 * Call it once from a holder's `init` or a screen's view setup, on the same view that is clickable
 * - focus and the click must live on one element, or the D-pad highlights what does not respond.
 */
fun View.bindTvFocus(scale: Float = FOCUS_SCALE) {
    if (!context.isTvUi) return

    //clickable views are focusable already; an explicit set keeps the contract with the plain Views
    isFocusable = true

    //the foreground of touch ripples, put back when the focus leaves - the border draws over it
    val restForeground = foreground
    setOnFocusChangeListener { view, hasFocus -> view.applyTvFocusVisual(hasFocus, scale, restForeground) }
}

private fun View.applyTvFocusVisual(hasFocus: Boolean, scale: Float, restForeground: android.graphics.drawable.Drawable?) {
    val grow = if (hasFocus) scale else 1.0f
    val lift = if (hasFocus) context.dp(FOCUS_LIFT_DP).toFloat() else 0f

    animate()
            .scaleX(grow)
            .scaleY(grow)
            .translationZ(lift)
            .setDuration(FOCUS_ANIMATION_MILLIS)
            .start()

    foreground = if (hasFocus) tvFocusBorder() else restForeground
}

/** The outline, two layers: an accent glow on the view's edge and a bright core inside it. Fresh
 * each focus, so a theme or ascent change mid-screen is picked up rather than kept from the view's
 * first bind. */
private fun View.tvFocusBorder(): Drawable {
    val glow = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.dp(BORDER_RADIUS_DP).toFloat()
        setStroke(context.dp(BORDER_GLOW_DP), context.colorAttr(R.attr.colorFocusBorder))
    }
    val core = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.dp(BORDER_RADIUS_DP - BORDER_CORE_INSET_DP).toFloat()
        setStroke(context.dp(BORDER_CORE_DP), Color.argb(BORDER_CORE_ALPHA, 255, 255, 255))
    }
    val inset = context.dp(BORDER_CORE_INSET_DP)
    return LayerDrawable(arrayOf(glow, core)).apply {
        setLayerInset(1, inset, inset, inset, inset)
    }
}

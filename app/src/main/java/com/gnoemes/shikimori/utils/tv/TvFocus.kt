package com.gnoemes.shikimori.utils.tv

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.Window
import android.widget.AbsListView
import android.widget.Button
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.utils.colorAttr
import com.gnoemes.shikimori.utils.dp
import com.gnoemes.shikimori.utils.isDarkTheme
import com.gnoemes.shikimori.utils.isTvUi

//The TV mode's visible focus, two layers. With no pointer, the user only knows where they are from
//the visuals of the view under focus, so all of them are tied to the ascent color:
//- the platform layer paints whatever takes focus in a window - the outline, and through the theme
//  (TvFocusOverlay, applied by Activity.setTheme) the fill - without any per-view wiring, so a
//  screen's elements are visible even where no one ever walked with the D-pad;
//- bindTvFocus wires particular views by hand for the grow-and-lift animation on top, the cards'
//  signature move. In the mobile mode neither layer happens - touches never move focus, and a
//  phone screen does not need it.

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

/** The bright core on the dark themes: white, near solid. */
private const val BORDER_CORE_ALPHA = 230

/** The dark themes read a white core; the light one's white surfaces swallow it, so it goes dark there. */
private const val BORDER_CORE_DARK_ALPHA = 160

/** Marks a view wired with [bindTvFocus]: it paints itself, the window's fallback layer skips it. */
private const val BOUND_TAG = R.id.tv_focus_bound

/** The visuals a fallback-painted view had at rest, kept while its focus lasts. */
private const val REST_TAG = R.id.tv_focus_rest

/** Marks the windows whose fallback layer is already installed. */
private const val PAINT_INSTALLED_TAG = R.id.tv_focus_paint_installed

/** The rest visuals of a view painted by the fallback layer. */
private class RestVisuals(val foreground: Drawable?, val background: Drawable?)

/**
 * The platform layer: every view that takes focus in this window gets the outline (and, through
 * the theme, the fill) without any wiring, so the D-pad never lands on an invisible element -
 * the class of bugs the per-view binding kept breeding on new screens. Views wired with
 * [bindTvFocus] paint themselves and are skipped here; the fallback adds no scale, so dense
 * screens keep their layout. Install once per window (activity, dialog) in the TV mode.
 */
fun Window.installTvFocusPaint() {
    if (!decorView.context.isTvUi) return
    if (decorView.getTag(PAINT_INSTALLED_TAG) == true) return
    decorView.setTag(PAINT_INSTALLED_TAG, true)
    decorView.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->
        oldFocus?.clearTvFocusFallback()
        newFocus?.applyTvFocusFallback()
    }
    //dialogs built and shown before this install (material-dialogs shows in onCreateDialog, auto-
    //focusing its first button): that first focus predates the listener, painted here directly
    decorView.findFocus()?.applyTvFocusFallback()
}

private fun View.applyTvFocusFallback() {
    if (getTag(BOUND_TAG) == true) return
    //scroll vessels take focus as navigation hubs; an outline over the whole list is noise
    if (!(isClickable || isLongClickable) || this is RecyclerView || this is AbsListView ||
            this is ViewPager || this is NestedScrollView || this is ScrollView) return
    val border = tvFocusBorder()
    setTag(REST_TAG, RestVisuals(foreground, background))
    //most views take the outline as the foreground, drawn over their content. Buttons in dialog
    //windows (API 27) silently drop a programmatically-set foreground, so they take it as an extra
    //background layer instead, over their own one
    if (this is Button) {
        background = LayerDrawable(arrayOf(background ?: ColorDrawable(Color.TRANSPARENT), border))
    } else {
        foreground = border
    }
}

private fun View.clearTvFocusFallback() {
    if (getTag(BOUND_TAG) == true) return
    //no rest tag means this view was never painted by the fallback layer
    val rest = getTag(REST_TAG) as? RestVisuals ?: return
    setTag(REST_TAG, null)
    foreground = rest.foreground
    background = rest.background
}

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

    //the window's fallback layer must not repaint this view: its own listener drives the grow too
    setTag(BOUND_TAG, true)

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

/** The outline, two layers: an accent glow on the view's edge and a bright core inside it - white on
 * the dark themes, near-black on the light one, where white would vanish on white. Fresh each focus,
 * so a theme or ascent change mid-screen is picked up rather than kept from the view's first bind. */
private fun View.tvFocusBorder(): Drawable {
    val glow = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.dp(BORDER_RADIUS_DP).toFloat()
        setStroke(context.dp(BORDER_GLOW_DP), context.colorAttr(R.attr.colorFocusBorder))
    }
    val coreColor = if (context.isDarkTheme) Color.argb(BORDER_CORE_ALPHA, 255, 255, 255)
    else Color.argb(BORDER_CORE_DARK_ALPHA, 0, 0, 0)
    val core = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = context.dp(BORDER_RADIUS_DP - BORDER_CORE_INSET_DP).toFloat()
        setStroke(context.dp(BORDER_CORE_DP), coreColor)
    }
    val inset = context.dp(BORDER_CORE_INSET_DP)
    return LayerDrawable(arrayOf(glow, core)).apply {
        setLayerInset(1, inset, inset, inset, inset)
    }
}

package com.gnoemes.shikimori.utils

import android.content.Context
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.utils.widgets.DebouncedOnClickListener
import com.gnoemes.shikimori.utils.widgets.DebouncedOnMenuClickListener
import com.google.android.material.snackbar.Snackbar

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.isVisible(): Boolean = visibility == View.VISIBLE

inline fun View.visibleIf(block: () -> Boolean) {
    if (block()) visible() else gone()
}

inline fun View.onClick(mills: Long = Constants.DEFAULT_DEBOUNCE_INTERVAL, crossinline l: (v: View) -> Unit) {
    setOnClickListener(object : DebouncedOnClickListener(mills) {
        override fun onDebouncedClick(v: View?) {
            if (v != null) l.invoke(v)
        }
    })
}

inline fun Toolbar.onMenuClick(mills: Long = Constants.DEFAULT_DEBOUNCE_INTERVAL, crossinline l: (item: MenuItem?) -> Boolean) {
    setOnMenuItemClickListener(object : DebouncedOnMenuClickListener(mills) {
        override fun onDebouncedClick(v: MenuItem?): Boolean = l.invoke(v)
    })
}

fun SwipeRefreshLayout.showRefresh() {
    isRefreshing = true
}

fun SwipeRefreshLayout.hideRefresh() {
    isRefreshing = false
}

fun ImageView.tint(@ColorInt colorInt: Int) = setColorFilter(colorInt)

fun ImageView.hasImage() = drawable != null

fun Toolbar.menuVisibleIf(block: () -> Boolean) {
    if (block.invoke()) showMenu()
    else hideMenu()
}

fun Toolbar.hideMenu() {
    this.menu?.iterator()?.forEach { menuItem -> menuItem.isVisible = false }
}

fun Toolbar.showMenu() {
    this.menu?.iterator()?.forEach { menuItem -> menuItem.isVisible = true }
}

/**
 * A number at the toolbar's end, such as how many clubs: one menu item, its title replaced each time.
 * Each screen added another item on every load, so a refresh piled them up (BUGS #45).
 */
fun Toolbar.showCount(count: Int) {
    if (menu.findItem(R.id.item_count) == null) inflateMenu(R.menu.menu_count)
    menu.findItem(R.id.item_count)?.title = count.toString()
}

/**
 * The app's look for a search field in a toolbar: no gap before the text, the hint in the faint
 * text colour and, with [closeButton], a padded close button in the toolbar's text colour.
 */
fun SearchView.applyAppStyle(startPaddingDp: Int = 0, closeButton: Boolean = true) {
    findViewById<SearchView.SearchAutoComplete>(R.id.search_src_text)?.apply {
        setPadding(context.dp(startPaddingDp), 0, context.dp(8), 0)
        setHintTextColor(context.colorStateList(context.attr(R.attr.colorOnPrimarySecondary).resourceId))
    }
    findViewById<LinearLayout>(R.id.search_edit_frame)?.apply {
        layoutParams = (layoutParams as? LinearLayout.LayoutParams)?.apply {
            marginStart = 0
            leftMargin = 0
        }
    }
    if (closeButton) {
        findViewById<ImageView>(R.id.search_close_btn)?.apply {
            setPadding(context.dp(12), 0, context.dp(12), 0)
            tint(context.colorAttr(R.attr.colorOnPrimary))
        }
    }
}


fun Snackbar.floatingStyle(context: Context, @DimenRes margins: Int = R.dimen.snackbar_margin, @DrawableRes background: Int = R.drawable.bg_snackbar): Snackbar {
    val params = this.view.layoutParams as ViewGroup.MarginLayoutParams
    val margin = context.dimen(margins).toInt()
    params.setMargins(margin, margin, margin, margin)
    this.view.findViewById<Button>(R.id.snackbar_action)?.run {
        isAllCaps = false
        letterSpacing = 0.03f
    }
    this.view.findViewById<TextView>(R.id.snackbar_text)?.setTextColor(context.color(R.color.player_controls))
    this.view.layoutParams = params
    this.view.background = context.drawable(background)
    return this
}

fun View.dp(dp: Int): Int = context.dp(dp)

@ColorInt
fun View.color(@ColorRes colorRes: Int): Int = context.color(colorRes)

/**
 * Hides the keyboard when it types into this fragment. Found by the fragment's own view, so a screen
 * and a bottom sheet work alike: a sheet is a window of its own, and the activity's focus is not in it
 * (BUGS #27).
 */
fun Fragment.hideSoftInput() {
    val window = view?.windowToken ?: return
    context?.inputMethodManager()?.hideSoftInputFromWindow(window, 0)
}





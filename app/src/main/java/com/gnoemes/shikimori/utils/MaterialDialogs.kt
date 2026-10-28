package com.gnoemes.shikimori.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import com.afollestad.materialdialogs.MaterialDialog
import com.gnoemes.shikimori.R

/**
 * Every material-dialogs dialog of the app starts here. On Amoled a dialog is as black as the screen
 * behind it, so it gets a thin line around it, like the cards there (`themeStrokeWidth`, 0 on the
 * other themes). The library paints the window with its own rounded GradientDrawable and has no border
 * setting, so the line goes onto that drawable (BUGS #17).
 */
fun Context.materialDialog(): MaterialDialog = MaterialDialog(this).apply {
    val border = context.dimenAttr(R.attr.themeStrokeWidth)
    if (border > 0) {
        val background = window?.decorView?.background
        //newer Android versions can hand the window background back wrapped in an inset
        val shape = (background as? InsetDrawable)?.drawable ?: background
        (shape as? GradientDrawable)?.setStroke(border, context.colorAttr(R.attr.colorDivider))
    }
}

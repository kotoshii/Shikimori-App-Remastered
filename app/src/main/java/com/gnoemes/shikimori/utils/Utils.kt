package com.gnoemes.shikimori.utils

import android.graphics.Bitmap


object Utils {

    fun getDominantColor(bitmap: Bitmap): Int {
        val newBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val color = newBitmap.getPixel(0, 0)
        newBitmap.recycle()
        return color
    }

    fun checkNeedIFrame(url: String): Boolean {
        return when {
            url.contains("aparat") -> false
            else -> true
        }
    }

}
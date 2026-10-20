package com.gnoemes.shikimori.utils.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.gnoemes.shikimori.R

private const val TAG = "OutsideActivity"

/**
 * Opens another app - a browser, a video player - and says so when no app installed can, instead of
 * crashing (a plain startActivity throws) or doing nothing (BUGS #23). Trying, rather than asking
 * first, is Android's own advice: the asking misses apps hidden by newer Android versions' package
 * visibility rules.
 *
 * @return whether an app was started
 */
fun Context.startOutsideActivity(intent: Intent, options: Bundle? = null): Boolean =
        try {
            startActivity(intent, options)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "no app for $intent", e)
            Toast.makeText(this, R.string.common_no_app_to_open, Toast.LENGTH_SHORT).show()
            false
        }

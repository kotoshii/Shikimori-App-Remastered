package com.gnoemes.shikimori.utils

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.view.inputmethod.InputMethodManager

fun Context.audioManager(): AudioManager =
        getSystemService(Context.AUDIO_SERVICE) as AudioManager

fun Context.clipboardManager(): ClipboardManager =
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

fun Context.downloadManager(): DownloadManager? =
        getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

fun Context.inputMethodManager(): InputMethodManager? =
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

fun Context.notificationManager(): NotificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

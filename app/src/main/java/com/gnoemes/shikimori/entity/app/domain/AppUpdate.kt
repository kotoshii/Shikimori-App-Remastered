package com.gnoemes.shikimori.entity.app.domain

/** A newer release than the installed one, as the last update check stored it. */
data class AppUpdate(
        val version: String,
        val changelog: String?,
        val apkUrl: String?
)

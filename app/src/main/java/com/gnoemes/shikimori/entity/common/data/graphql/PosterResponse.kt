package com.gnoemes.shikimori.entity.common.data.graphql

import com.google.gson.annotations.SerializedName

/**
 * `Poster` type of the Shikimori GraphQL schema.
 *
 * Since 2024 the REST API no longer exposes posters of newly added content - it answers with
 * `/assets/globals/missing_*.jpg` placeholders instead, because poster files are not named after
 * the content id anymore. GraphQL is the only place where the real file names are published.
 *
 * `*Alt*` fields are jpeg/png renditions of the webp ones. They are preferred because the app
 * supports API 16, while lossless/transparent webp requires API 18.
 *
 * Measured sizes (identical to their REST counterparts unless stated):
 * - [mainUrl] 225x320 - REST `original` is 225x318
 * - [previewUrl] 160x226 - REST `preview` is 160x226
 * - [mini2xUrl] 120x188 - closest to REST `x96` (96x150)
 * - [miniUrl] 60x94 - closest to REST `x48` (48x75)
 * - [originalUrl] the untouched upload, considerably larger than REST `original`
 */
data class PosterResponse(
        @field:SerializedName("originalUrl") val originalUrl: String?,
        @field:SerializedName("mainUrl") val mainUrl: String?,
        @field:SerializedName("mainAltUrl") val mainAltUrl: String?,
        @field:SerializedName("previewUrl") val previewUrl: String?,
        @field:SerializedName("previewAltUrl") val previewAltUrl: String?,
        @field:SerializedName("mini2xUrl") val mini2xUrl: String?,
        @field:SerializedName("miniAlt2xUrl") val miniAlt2xUrl: String?,
        @field:SerializedName("miniUrl") val miniUrl: String?,
        @field:SerializedName("miniAltUrl") val miniAltUrl: String?
) {

    private val main: String?
        get() = mainAltUrl ?: mainUrl ?: originalUrl

    private val preview: String?
        get() = previewAltUrl ?: previewUrl ?: main

    private val mini2x: String?
        get() = miniAlt2xUrl ?: mini2xUrl ?: preview

    private val mini: String?
        get() = miniAltUrl ?: miniUrl ?: mini2x

    /**
     * Poster url matching the REST image size named [size], e.g. `original`, `preview`, `x96`.
     */
    fun urlFor(size: String?): String? = when (size) {
        "original", "main" -> main
        "preview", "x160", "x148" -> preview
        "x96", "x80", "x73", "x64" -> mini2x
        "x48", "x32", "x16" -> mini
        else -> preview
    }

    companion object {
        /** Marker for "asked Shikimori, it has no poster either" - never handed out as a result. */
        val EMPTY = PosterResponse(null, null, null, null, null, null, null, null, null)
    }
}

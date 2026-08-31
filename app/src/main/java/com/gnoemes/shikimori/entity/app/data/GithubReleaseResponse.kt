package com.gnoemes.shikimori.entity.app.data

import com.google.gson.annotations.SerializedName

/**
 * Subset of a GitHub release. `releases/latest` skips drafts and pre-releases, so whatever comes
 * back is the newest published build.
 *
 * [body] is the release notes markdown, shown by the changelog dialog through `MarkdownRenderer`.
 * It is an empty string for a release published without notes - v0.8.6.51 is one - so the dialog
 * has to cope with nothing to show.
 */
data class GithubReleaseResponse(
        @field:SerializedName("tag_name") val tag: String?,
        @field:SerializedName("html_url") val url: String?,
        @field:SerializedName("body") val body: String?,
        @field:SerializedName("assets") val assets: List<GithubAssetResponse>?
) {

    /**
     * Where to download the build from. Every release of this fork attaches exactly one apk, named
     * `ShikimoriApp-v<version>-release.apk`, but it is matched by extension rather than by that
     * name so a rename does not silently disable updating.
     */
    val apkUrl: String?
        get() = assets?.firstOrNull { it.name?.endsWith(".apk", ignoreCase = true) == true }?.downloadUrl
}

data class GithubAssetResponse(
        @field:SerializedName("name") val name: String?,
        @field:SerializedName("browser_download_url") val downloadUrl: String?,
        @field:SerializedName("size") val size: Long?
)

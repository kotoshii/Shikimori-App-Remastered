package com.gnoemes.shikimori.entity.app.data

import com.google.gson.annotations.SerializedName

/**
 * Subset of a GitHub release. `releases/latest` skips drafts and pre-releases, so whatever comes
 * back is the newest published build.
 */
data class GithubReleaseResponse(
        @field:SerializedName("tag_name") val tag: String?,
        @field:SerializedName("html_url") val url: String?
)

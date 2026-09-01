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

    /**
     * The part of the notes the changelog dialog shows.
     *
     * Releases carry russian first and english after [APP_CUT_MARKER], because the audience is
     * mostly russian speaking while the repository is read by both. The app shows the russian half
     * only - the dialog is small and has no language switch, and translating twice inside it would
     * be twice the scrolling.
     *
     * The marker is an html comment, so github renders nothing where it sits and the release page
     * still reads as one document; the api hands back the raw markdown, comment included.
     *
     * ⚠️ **Fails open on purpose.** Splitting on a marker that is not there yields the whole string,
     * so a release written without one - every release before 0.9.0 - still shows its notes in full
     * rather than nothing.
     *
     * Spacing and case inside the comment are ignored, because the marker is typed by hand into a
     * github release form: `<!--app:cut-->` and `<!-- APP:CUT -->` cut just as well. Getting it
     * slightly wrong would otherwise fail silently, and the failure looks like english appearing in
     * the dialog rather than like a typo.
     */
    val localizedBody: String?
        get() = body?.split(APP_CUT_MARKER, limit = 2)?.first()?.trim()

    companion object {
        //see BUILD_AND_RELEASE.md, which pins this into the release notes template
        private val APP_CUT_MARKER = Regex("<!--\\s*app:cut\\s*-->", RegexOption.IGNORE_CASE)
    }
}

data class GithubAssetResponse(
        @field:SerializedName("name") val name: String?,
        @field:SerializedName("browser_download_url") val downloadUrl: String?,
        @field:SerializedName("size") val size: Long?
)

package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import com.gnoemes.shikimori.di.app.annotations.SettingsQualifier
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.entity.series.data.kodik.KodikLinksResponse
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.utils.putString
import javax.inject.Inject

/**
 * Kodik gives up its streams in three steps.
 *
 * 1. The embed page carries a signed request context - `d`, `pd` and `ref` with a signature each -
 *    plus the `vInfo` triple identifying the video. Both sit in plain `var` blocks in the html.
 * 2. Those are posted back to the player's link endpoint, which answers with one url per quality.
 * 3. Every url is rotated and base64'd and has to be decoded.
 *
 * The page renders only inside an iframe, and says "Извините, но данной страницы не существует"
 * even when the fetch succeeded - never treat that text as a failure, check the parsed params.
 */
class KodikParserImpl @Inject constructor(
        @SettingsQualifier private val prefs: SharedPreferences
) : KodikParser {

    companion object {
        /**
         * The player reads its endpoint from `atob("L2Z0b3I=")` inside `app.player_single.<sha>.js`.
         * It is base64'd there precisely so it can be moved, so this is only the opening guess -
         * when it stops answering, [rememberLinksUrl] re-reads it from the script and stores the
         * new one.
         */
        private const val DEFAULT_LINKS_PATH = "/ftor"

        /**
         * Form field name to the javascript variable holding it. Only `d` is spelled differently,
         * the rest match. Every value is signed, so all six have to be sent back exactly as the
         * page handed them over - `ref` is regularly an empty string and still has to be included.
         */
        private val SIGNED_PARAMS = mapOf(
                "d" to variableRegex("domain"),
                "d_sign" to variableRegex("d_sign"),
                "pd" to variableRegex("pd"),
                "pd_sign" to variableRegex("pd_sign"),
                "ref" to variableRegex("ref"),
                "ref_sign" to variableRegex("ref_sign")
        )

        //used to be videoInfo.*, renamed to vInfo.* - which is why the old server side parse broke
        private val VIDEO_INFO_REGEX = "vInfo\\.(type|hash|id)\\s*=\\s*'([^']*)'".toRegex()

        private val PLAYER_SCRIPT_REGEX = "src=\"(/[^\"]*app\\.player_single\\.[^\"]+\\.js)\"".toRegex()

        private val LINKS_PATH_REGEX = "url\\s*:\\s*atob\\(\"([^\"]+)\"\\)".toRegex()

        private fun variableRegex(name: String) = "var\\s+$name\\s*=\\s*\"([^\"]*)\"".toRegex()
    }

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    /**
     * Everything the link call wants. An empty map means the page was not what was expected, and
     * the caller should give up rather than post a half filled form - the signatures are worthless
     * without each other.
     */
    override fun linkRequestParams(html: String?): Map<String, String> {
        if (html.isNullOrEmpty()) return emptyMap()

        val params = mutableMapOf<String, String>()

        for ((field, regex) in SIGNED_PARAMS) {
            params[field] = regex.find(html)?.groupValues?.get(1) ?: return emptyMap()
        }

        val info = VIDEO_INFO_REGEX.findAll(html).associate { it.groupValues[1] to it.groupValues[2] }
        if (!info.containsKey("type") || !info.containsKey("hash") || !info.containsKey("id")) return emptyMap()
        params.putAll(info)

        //what the player itself sends: no ad blocking suspicion, cdn assumed up, no user data
        params["bad_user"] = "false"
        params["cdn_is_working"] = "true"
        params["info"] = "{}"

        return params
    }

    /**
     * The path is kept in preferences rather than in memory, so a path learned the hard way is
     * still known after the app is restarted - otherwise every cold start would pay for the
     * discovery again. `SharedPreferences` holds its contents in memory once loaded, so this does
     * not touch the disk on the way in.
     */
    override fun linksUrl(playerUrl: String): String? {
        val path = prefs.getString(SettingsExtras.KODIK_LINKS_PATH, DEFAULT_LINKS_PATH)
                ?: DEFAULT_LINKS_PATH

        return origin(playerUrl)?.plus(path)
    }

    override fun playerScriptUrl(html: String?, playerUrl: String): String? {
        if (html.isNullOrEmpty()) return null

        val path = PLAYER_SCRIPT_REGEX.find(html)?.groupValues?.get(1) ?: return null
        return origin(playerUrl)?.plus(path)
    }

    /**
     * Called when the known endpoint stopped answering. Re-reads it from the player script and
     * stores it, so neither the rest of this run nor any later one pays for the discovery again.
     * Returns null when the script did not hold one either, which means the flow changed more
     * deeply than a moved path.
     */
    override fun rememberLinksUrl(playerScript: String?, playerUrl: String): String? {
        if (playerScript.isNullOrEmpty()) return null

        val encoded = LINKS_PATH_REGEX.find(playerScript)?.groupValues?.get(1) ?: return null

        val path = try {
            String(Base64.decode(encoded, Base64.DEFAULT))
        } catch (e: IllegalArgumentException) {
            return null
        }

        if (!path.startsWith("/")) return null

        prefs.putString(SettingsExtras.KODIK_LINKS_PATH, path)
        return origin(playerUrl)?.plus(path)
    }

    /**
     * One media playlist per quality - not a master playlist, so each quality is its own track and
     * the quality menu maps one to one. Sound is muxed in, no separate audio file.
     */
    override fun tracks(response: KodikLinksResponse?): List<Track> {
        val links = response?.links ?: return emptyList()

        return links
                .mapNotNull { (quality, sources) ->
                    val src = sources.firstOrNull()?.src ?: return@mapNotNull null
                    val url = decodeLink(src) ?: return@mapNotNull null

                    Track(quality, if (url.startsWith("http")) url else "https:$url")
                }
                .sortedByDescending { it.quality.toIntOrNull() ?: 0 }
    }

    /**
     * Links come rotated and then base64'd. The rotation is not fixed - the old server side parser
     * hardcoded 13, the player used 18 when this was written - so every shift is tried and the one
     * that decodes to something url shaped wins. Costs 26 base64 decodes of a short string.
     */
    private fun decodeLink(src: String): String? {
        for (shift in 0..25) {
            val decoded = try {
                String(Base64.decode(rotate(src, shift), Base64.DEFAULT))
            } catch (e: IllegalArgumentException) {
                continue
            }

            if (isLink(decoded)) return decoded
        }

        return null
    }

    private fun rotate(src: String, shift: Int): String =
            src.map { char ->
                when (char) {
                    in 'a'..'z' -> 'a' + (char - 'a' + shift) % 26
                    in 'A'..'Z' -> 'A' + (char - 'A' + shift) % 26
                    else -> char
                }
            }.joinToString("")

    private fun isLink(decoded: String): Boolean =
            (decoded.startsWith("//") || decoded.startsWith("http")) &&
                    decoded.all { it.toInt() in 32..126 }

    private fun origin(url: String): String? {
        val uri = Uri.parse(if (url.startsWith("//")) "https:$url" else url)
        val host = uri.host ?: return null

        return "${uri.scheme ?: "https"}://$host"
    }
}

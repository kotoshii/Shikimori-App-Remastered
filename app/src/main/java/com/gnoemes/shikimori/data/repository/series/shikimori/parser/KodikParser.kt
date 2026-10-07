package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.di.app.annotations.SettingsQualifier
import com.gnoemes.shikimori.entity.app.domain.SettingsExtras
import com.gnoemes.shikimori.entity.series.data.kodik.KodikLinksResponse
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import com.gnoemes.shikimori.utils.putString
import io.reactivex.Single
import retrofit2.HttpException
import retrofit2.Response
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
class KodikParser @Inject constructor(
        private val api: VideoApi,
        @SettingsQualifier private val prefs: SharedPreferences
) : HostingParser {

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

    override val hosting = VideoHosting.KODIK::class.java

    override fun parse(playerUrl: String): Single<ParsedVideo> =
            api.getPlayerHtml(playerUrl)
                    .flatMap { getLinks(it.string(), playerUrl) }
                    .map { ParsedVideo(tracks(it)) }

    /**
     * Kodik keeps the path it serves links from base64'd inside its player script, so that it can
     * be moved. The known one is tried first and the script is only read when the answer says the
     * path is wrong, because reading it means downloading the player bundle (47 KB gzipped) that
     * would otherwise be paid for on every playback.
     */
    private fun getLinks(html: String, playerUrl: String): Single<KodikLinksResponse> {
        val params = linkRequestParams(html)
        val url = linksUrl(playerUrl)

        if (params.isEmpty() || url == null) {
            return Single.error(IllegalStateException("kodik player page holds no request params"))
        }

        return api.getKodikLinks(url, params)
                .flatMap { response ->
                    when {
                        hasLinks(response) -> Single.just(response.body()!!)
                        looksLikeMovedEndpoint(response) -> retryFromPlayerScript(html, playerUrl, url, params, response)
                        else -> Single.error<KodikLinksResponse>(linksError(url, response))
                    }
                }
    }

    private fun hasLinks(response: Response<KodikLinksResponse>): Boolean =
            response.isSuccessful && response.body()?.links?.isNotEmpty() == true

    /**
     * A path that is simply gone answers **404**. A path that was retired but left routed answers
     * **200 with an empty body** - which is exactly what the previous endpoint, `/tru`, still does.
     * Both mean the same thing, so both send us to the player script to find where it went.
     *
     * Everything else - a timeout, a dropped connection, a 5xx - is deliberately *not* treated this
     * way. Re-reading the script would not help, and the real error stays intact.
     */
    private fun looksLikeMovedEndpoint(response: Response<KodikLinksResponse>): Boolean =
            response.code() == 404 || response.isSuccessful

    private fun retryFromPlayerScript(
            html: String,
            playerUrl: String,
            triedUrl: String,
            params: Map<String, String>,
            failed: Response<KodikLinksResponse>
    ): Single<KodikLinksResponse> {
        val scriptUrl = playerScriptUrl(html, playerUrl)
                ?: return Single.error(linksError(triedUrl, failed))

        return api.getTextResponse(scriptUrl)
                .flatMap { script ->
                    val movedUrl = rememberLinksUrl(script.string(), playerUrl)

                    //the script naming the path we just tried means the endpoint was never the
                    //problem - a 404 also comes back when the posted params are not accepted
                    if (movedUrl == null || movedUrl == triedUrl) {
                        Single.error<Response<KodikLinksResponse>>(linksError(triedUrl, failed))
                    } else {
                        api.getKodikLinks(movedUrl, params)
                    }
                }
                .flatMap { retried ->
                    if (hasLinks(retried)) Single.just(retried.body()!!)
                    else Single.error<KodikLinksResponse>(linksError(triedUrl, retried))
                }
    }

    private fun linksError(url: String, response: Response<KodikLinksResponse>): Throwable =
            if (response.isSuccessful) IllegalStateException("kodik returned no links from $url")
            else HttpException(response)

    /**
     * Everything the link call wants. An empty map means the page was not what was expected, and
     * the caller should give up rather than post a half filled form - the signatures are worthless
     * without each other.
     */
    private fun linkRequestParams(html: String?): Map<String, String> {
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
    private fun linksUrl(playerUrl: String): String? {
        val path = prefs.getString(SettingsExtras.KODIK_LINKS_PATH, DEFAULT_LINKS_PATH)
                ?: DEFAULT_LINKS_PATH

        return origin(playerUrl)?.plus(path)
    }

    private fun playerScriptUrl(html: String?, playerUrl: String): String? {
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
    private fun rememberLinksUrl(playerScript: String?, playerUrl: String): String? {
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
    private fun tracks(response: KodikLinksResponse?): List<Track> {
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

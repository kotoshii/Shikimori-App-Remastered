package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.entity.series.data.CdaApiRequest
import com.gnoemes.shikimori.entity.series.data.CdaPlayerData
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import com.gnoemes.shikimori.entity.series.domain.sortedByQuality
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.HttpUrl
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import javax.inject.Inject

class CdaParser @Inject constructor(
        private val api: VideoApi
) : HostingParser {

    override val hosting = VideoHosting.CDA::class.java

    override fun parse(playerUrl: String): Single<ParsedVideo> =
            api.getPlayerHtml(playerUrl)
                    .flatMap { html ->
                        val video = getPlayerData(html.string())?.video
                                ?: return@flatMap Single.just(ParsedVideo.EMPTY)
                        val manifestUrl = video.manifest

                        if (manifestUrl != null) api.getTextResponse(manifestUrl)
                                .map { ParsedVideo(manifestTracks(it.string(), manifestUrl)) }
                        else linkTracks(video).map { ParsedVideo(it) }
                    }

    /** The player page keeps its configuration in a `player_data` attribute. */
    private fun getPlayerData(html: String?): CdaPlayerData? {
        if (html.isNullOrEmpty()) return null

        val playerDataJson = Jsoup.parse(html)
                .select(".brdPlayer > div")
                .first()
                ?.attr("player_data")
                ?: return null

        return try {
            Gson().fromJson(playerDataJson, CdaPlayerData::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * Most videos have no manifest any more, only `file`: an hls playlist of one quality. `videoGetLink`
     * gives a direct mp4 with sound for each listed quality, which makes the quality menu (BUGS #19,
     * checked live 2026-10-24); `file` is the one track left when it gives none. A video with one
     * quality lists it as «480p» whatever it is, so its label comes from the video's real height.
     */
    private fun linkTracks(video: CdaPlayerData.Video): Single<List<Track>> {
        val qualities = video.cdaQualities
        val height = video.height?.toString() ?: Track.UNKNOWN_QUALITY

        return Observable.fromIterable(qualities)
                .flatMapMaybe { (quality, code) -> videoLink(video, code, if (qualities.size == 1) height else quality) }
                .toList()
                .map { tracks ->
                    if (tracks.isNotEmpty()) tracks.sortedByQuality()
                    else listOfNotNull(video.file?.let { Track(height, it) })
                }
    }

    private fun videoLink(video: CdaPlayerData.Video, code: String, quality: String): Maybe<Track> =
            api.getCdaVideoLink(CdaApiRequest(1, "2.0", "videoGetLink", listOf(video.id, code, video.ts, video.hash2)))
                    .flatMapMaybe { response ->
                        //a code the video does not list gets an address with no host, "https://.cda.pl/…"
                        val url = response.result?.takeIf { it.status == "ok" }?.resp?.takeIf { HttpUrl.parse(it) != null }
                        if (url == null) Maybe.empty() else Maybe.just(Track(quality, url))
                    }

    /**
     * The manifest lists a separate mp4 per quality, which is what the quality menu needs. Those files
     * carry video only - the sound sits in its own representation - so every track also points at the
     * audio file and the player merges the two.
     */
    private fun manifestTracks(manifestXml: String?, manifestUrl: String?): List<Track> {
        if (manifestXml.isNullOrEmpty() || manifestUrl == null) return emptyList()

        val manifest = Jsoup.parse(manifestXml, "", Parser.xmlParser())
        val base = manifestUrl.substringBeforeLast('/')

        val audioUrl = manifest.select("AdaptationSet[contentType=audio] Representation > BaseURL")
                .first()
                ?.text()
                ?.let { "$base/$it" }

        return manifest.select("AdaptationSet[contentType=video] Representation")
                .mapNotNull { representation ->
                    val quality = representation.attr("height").nullIfEmpty() ?: return@mapNotNull null
                    val file = representation.select("BaseURL").first()?.text() ?: return@mapNotNull null

                    Track(quality, "$base/$file", audioUrl)
                }
                .sortedByQuality()
    }

    private fun String.nullIfEmpty(): String? = if (isEmpty()) null else this
}

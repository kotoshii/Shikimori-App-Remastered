package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.entity.series.data.CdaPlayerData
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import io.reactivex.Single
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
                        val manifestUrl = getManifestUrl(html.string())
                                ?: return@flatMap Single.just(ParsedVideo.EMPTY)

                        api.getTextResponse(manifestUrl)
                                .map { ParsedVideo(tracks(it.string(), manifestUrl)) }
                    }

    /**
     * The player page keeps its configuration in a `player_data` attribute. `file` used to hold a
     * direct link and `videoGetLink` handed out one file per quality - both are gone, the field is
     * empty and the api answers with the dash manifest whatever quality is asked for.
     */
    private fun getManifestUrl(html: String?): String? {
        if (html.isNullOrEmpty()) return null

        val playerDataJson = Jsoup.parse(html)
                .select(".brdPlayer > div")
                .first()
                ?.attr("player_data")
                ?: return null

        return try {
            Gson().fromJson(playerDataJson, CdaPlayerData::class.java)?.video?.manifest
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * The manifest still lists a separate mp4 per quality, which is what the quality menu needs.
     * Those files carry video only - the sound sits in its own representation - so every track also
     * points at the audio file and the player merges the two.
     */
    private fun tracks(manifestXml: String?, manifestUrl: String?): List<Track> {
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
                .sortedByDescending { it.quality.toIntOrNull() ?: 0 }
    }

    private fun String.nullIfEmpty(): String? = if (isEmpty()) null else this
}

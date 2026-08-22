package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.data.CdaPlayerData
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import javax.inject.Inject

class CdaParserImpl @Inject constructor() : CdaParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    /**
     * The player page keeps its configuration in a `player_data` attribute. `file` used to hold a
     * direct link and `videoGetLink` handed out one file per quality - both are gone, the field is
     * empty and the api answers with the dash manifest whatever quality is asked for.
     */
    override fun getManifestUrl(html: String?): String? {
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
    override fun tracks(manifestXml: String?, manifestUrl: String?): List<Track> {
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

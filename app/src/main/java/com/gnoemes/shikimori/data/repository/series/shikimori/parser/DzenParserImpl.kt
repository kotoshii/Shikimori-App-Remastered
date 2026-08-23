package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.data.DzenPlayerData
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject

class DzenParserImpl @Inject constructor() : DzenParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    /**
     * The page used to expose `Dzen.player.init(...)` with a `master.m3u8` stream, both are gone.
     * Streams now sit in a `var _params=({...})` block, and the progressive ones are plain mp4 files
     * with the sound already inside, one per quality.
     *
     * The links are signed for the address and the user agent that asked for the page, so playback
     * has to reuse the same one, see `Utils.getRequestHeadersForHosting`.
     */
    override fun tracks(html: String?): List<Track> {
        if (html.isNullOrEmpty()) return emptyList()

        val params = extractParams(html) ?: return emptyList()

        val streams = try {
            Gson().fromJson(params, DzenPlayerData::class.java)?.ssrData?.exportResponse?.content?.streams
        } catch (e: JsonSyntaxException) {
            null
        } ?: return emptyList()

        return streams
                .mapNotNull { stream ->
                    val url = stream.url ?: return@mapNotNull null
                    //hls and dash are adaptive playlists, the quality menu needs the plain files
                    val quality = QUALITIES[stream.type] ?: return@mapNotNull null

                    Track(quality, url)
                }
                .sortedByDescending { it.quality.toIntOrNull() ?: 0 }
    }

    /**
     * The page carries several `_params` blocks and only one of them holds the streams, so each is
     * read in turn until the right one shows up.
     */
    private fun extractParams(html: String): String? {
        var searchFrom = 0

        while (true) {
            val marker = html.indexOf(PARAMS_MARKER, searchFrom)
            if (marker < 0) return null

            val objectStart = html.indexOf('{', marker)
            if (objectStart < 0) return null

            val json = readJsonObject(html, objectStart)
            if (json != null && json.contains(SSR_DATA_MARKER)) return json

            searchFrom = objectStart + 1
        }
    }

    /** Reads one balanced `{...}` starting at [start], ignoring braces inside strings. */
    private fun readJsonObject(text: String, start: Int): String? {
        var depth = 0
        var inString = false
        var escaped = false

        for (i in start until text.length) {
            val symbol = text[i]

            when {
                escaped -> escaped = false
                inString && symbol == BACKSLASH -> escaped = true
                symbol == '"' -> inString = !inString
                inString -> Unit
                symbol == '{' -> depth++
                symbol == '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }

        return null
    }

    companion object {
        private const val PARAMS_MARKER = "_params"
        private const val SSR_DATA_MARKER = "\"ssrData\""
        private const val BACKSLASH = '\\'

        //dzen names its qualities the ok.ru way instead of by height
        private val QUALITIES = mapOf(
                "tiny" to "144",
                "lowest" to "240",
                "low" to "360",
                "medium" to "480",
                "high" to "720",
                "fullhd" to "1080"
        )
    }
}

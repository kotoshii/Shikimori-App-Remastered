package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import android.util.Base64
import com.gnoemes.shikimori.entity.series.data.MatreshkaPlaylistData
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import javax.inject.Inject

class MatreshkaParserImpl @Inject constructor() : MatreshkaParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    /**
     * Shikicinema hands out matreshka links already in embed form
     * (`https://matreshka.tv/embed/video/<id>`). The page is server rendered by Nuxt and every
     * stream url is somewhere in the `window.__NUXT__` payload, one signed `master.m3u8` per
     * quality plus an adaptive one.
     *
     * ⚠ The urls are read straight out of the page rather than from the `abr.h264.playlists` map
     * that lists them, because that map is not always readable. Nuxt hoists any value it sees twice
     * into a variable, and a video that also has an av1 encode carries the same four urls under
     * both codecs - so `playlists` degrades from urls to variable names (`{"360":dP,"480":dQ,…}`)
     * exactly when av1 exists. Every url is still spelled out somewhere in the payload, and each one
     * describes itself: the base64 segment in the middle of it decodes to
     * `{"id":…,"codec":"h264","userQuality":[720]}`, which is where the quality and the codec come
     * from. See [MatreshkaPlaylistData].
     *
     * **Only h264 is taken.** Android 8.1, the oldest version this fork supports, has no av1
     * decoder at all. A video published as av1 only therefore yields no tracks and falls back to
     * the web player, which is the right outcome rather than a black screen.
     *
     * ⚠ Read the per-quality urls, **not** the adaptive one. The adaptive playlist lists only 360,
     * 480 and 720 - **1080 exists solely as its own url**, which is why the site loads a separate
     * playlist when a viewer picks it.
     *
     * Each url is a master playlist holding exactly one variant, which points at a plain VOD media
     * playlist of mpeg-ts segments. The player needs nothing else, and VideoFileDownloader already
     * follows that single master to variant hop.
     *
     * The links are signed, so they cannot be built by hand - but the signature covers only the path
     * and the expiry, not the address or the user agent that asked, so playback needs no headers.
     * They stay valid until the end of the following day.
     *
     * A removed or blocked video still answers 200 with a page that carries no stream urls at all,
     * and produces no tracks - the web player stays available for it.
     */
    override fun tracks(html: String?): List<Track> {
        if (html.isNullOrEmpty()) return emptyList()

        //the payload escapes every slash, and the urls are easier to match once it does not
        val payload = html.replace(ESCAPED_SLASH, "/")

        return PLAYLIST_URL.findAll(payload)
                .mapNotNull { match ->
                    val playlist = describe(match.groupValues[1]) ?: return@mapNotNull null
                    if (playlist.codec != H264) return@mapNotNull null

                    val quality = playlist.quality ?: return@mapNotNull null
                    Track(quality.toString(), match.value)
                }
                .distinctBy { it.quality }
                .sortedByDescending { it.quality.toIntOrNull() ?: 0 }
                .toList()
    }

    /** Reads the base64'd description in the middle of a stream url. */
    private fun describe(segment: String): MatreshkaPlaylistData? =
            try {
                val json = String(Base64.decode(segment, Base64.DEFAULT), Charsets.UTF_8)
                Gson().fromJson(json, MatreshkaPlaylistData::class.java)
            } catch (e: IllegalArgumentException) {
                //the segment was not base64 after all
                null
            } catch (e: JsonSyntaxException) {
                null
            }

    companion object {
        private const val ESCAPED_SLASH = "\\u002F"
        private const val H264 = "h264"

        //  https://<cdn>/hm/<channel>/<base64 description>/master.m3u8?expires=…&md5=…
        private val PLAYLIST_URL = Regex("https://[^\"]*?/([^/\"]+)/master\\.m3u8[^\"]*")
    }
}

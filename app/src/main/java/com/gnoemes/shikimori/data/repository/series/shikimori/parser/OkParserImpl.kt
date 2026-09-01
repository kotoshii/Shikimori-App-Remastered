package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import android.os.Build
import androidx.annotation.RequiresApi
import com.gnoemes.shikimori.entity.series.data.OkPlayerData
import com.gnoemes.shikimori.entity.series.data.OkPlayerFlashvarsMetadata
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import io.lindstrom.m3u8.parser.MasterPlaylistParser
import io.lindstrom.m3u8.parser.ParsingMode
import org.jsoup.Jsoup
import javax.inject.Inject

class OkParserImpl @Inject constructor() : OkParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    /**
     * Everything the player needs sits in the `data-options` attribute of the player element, under
     * `flashvars.metadata`.
     *
     * ⚠️ **`metadata` used to be a json string and is now the object itself.** Reading it as a
     * string is what broke ok.ru in 2026-09: gson met an object where a `String` field was declared
     * and threw. Both shapes are accepted so the parser survives ok.ru changing its mind back.
     *
     * ⚠️ **A blocked or deleted video still answers 200**, with a page carrying no player element at
     * all — «Видео заблокировано из-за нарушений авторских прав» in the body. That produces no
     * tracks and the user falls back to the web player, where the message explains itself. A missing
     * player element is *not* evidence that ok.ru changed its markup; check the page text first.
     *
     * `videos[]` is now a single `full` rendition, so this is only the fallback — the real quality
     * ladder comes from [getMasterPlaylistUrl].
     *
     * The urls are signed for the requesting address but **not** for the user agent — they serve to
     * any agent and to none — so there is no `Utils.getRequestHeadersForHosting` entry.
     */
    override fun tracks(html: String?): List<Track> =
            metadata(html)
                    ?.videos
                    .orEmpty()
                    .mapNotNull { video ->
                        val quality = getResolution(video.name) ?: return@mapNotNull null
                        val url = video.url ?: return@mapNotNull null

                        Track(quality, url)
                    }
                    .sorted()

    override fun getMasterPlaylistUrl(html: String?): String? {
        //MasterPlaylistParser needs java 8 apis. Below N the progressive list is all this can offer,
        //and saying so here keeps the version check out of the repository.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null

        return metadata(html)?.ondemandHls?.takeIf { it.isNotEmpty() }
    }

    /**
     * ok.ru still encodes the whole 144 → 1080 ladder and only stopped publishing it as progressive
     * files, so the master playlist is where the quality menu comes from. Heights are read from the
     * variants themselves rather than from the `QUALITY=` names beside them, which differ from the
     * progressive ones (`medium/high/fullhd` against `sd/hd/full`).
     *
     * Each variant is a plain VOD playlist of mpeg-ts segments, which `VideoFileDownloader` already
     * walks and remuxes.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    override fun tracks(m3uContent: String?, masterPlaylistUrl: String?): List<Track> {
        if (m3uContent.isNullOrEmpty() || masterPlaylistUrl.isNullOrEmpty()) return emptyList()

        //⚠️ LENIENT is required, not a precaution: ok.ru writes a non-standard QUALITY= attribute on
        //every variant, and the strict parser SovetRomantica uses rejects the whole playlist for it
        val playlist = try {
            MasterPlaylistParser(ParsingMode.LENIENT).readPlaylist(m3uContent.replace("\r", ""))
        } catch (e: Exception) {
            return emptyList()
        }

        return playlist.variants()
                .mapNotNull { variant ->
                    val resolution = variant.resolution()
                    if (!resolution.isPresent) return@mapNotNull null

                    Track(resolution.get().height().toString(), resolve(masterPlaylistUrl, variant.uri()))
                }
                .sorted()
    }

    /** Variant uris are relative to the master playlist. */
    private fun resolve(masterPlaylistUrl: String, uri: String): String =
            if (uri.startsWith("http")) uri
            else masterPlaylistUrl.split("/").dropLast(1).plusElement(uri).joinToString("/")

    private fun metadata(html: String?): OkPlayerFlashvarsMetadata? {
        if (html.isNullOrEmpty()) return null

        val options = Jsoup.parse(html)
                .select(PLAYER_QUERY)
                .firstOrNull()
                ?.attr(PLAYER_OPTIONS_ATTRIBUTE)
                ?.takeIf { it.isNotEmpty() }
                ?: return null

        val gson = Gson()
        val metadata = try {
            gson.fromJson(options, OkPlayerData::class.java)?.flashvars?.metadata
        } catch (e: JsonSyntaxException) {
            null
        } ?: return null

        return read(gson, metadata)
    }

    /** Reads the metadata whether ok.ru nested it as an object or wrote it as a json string. */
    private fun read(gson: Gson, metadata: JsonElement): OkPlayerFlashvarsMetadata? =
            try {
                if (metadata.isJsonPrimitive) gson.fromJson(metadata.asString, OkPlayerFlashvarsMetadata::class.java)
                else gson.fromJson(metadata, OkPlayerFlashvarsMetadata::class.java)
            } catch (e: JsonSyntaxException) {
                null
            }

    private fun List<Track>.sorted(): List<Track> =
            distinctBy { it.quality }.sortedByDescending { it.quality.toIntOrNull() ?: 0 }

    private fun getResolution(okQuality: String?): String? {
        return when (okQuality) {
            "mobile" -> "144"
            "lowest" -> "240"
            "low" -> "360"
            "sd" -> "480"
            "hd" -> "720"
            "full" -> "1080"
            else -> null
        }
    }

    companion object {
        private const val PLAYER_QUERY = "div[data-module=\"OKVideo\"]"
        private const val PLAYER_OPTIONS_ATTRIBUTE = "data-options"
    }
}

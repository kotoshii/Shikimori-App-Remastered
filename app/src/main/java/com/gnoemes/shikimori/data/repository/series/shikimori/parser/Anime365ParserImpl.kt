package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import android.net.Uri
import com.gnoemes.shikimori.entity.series.data.anime365.Anime365VideoResponse
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import javax.inject.Inject

/**
 * Anime365 hands over its streams through a single authenticated call - no page scraping, no
 * signing. Given an embed link like `http://smotret-anime.org/translations/embed/828439`, the
 * quality list comes from `https://smotret-anime.org/api/translations/embed/828439?access_token=…`.
 *
 * This used to run on the Shimori backend, which was only ever a pass-through: it took the token
 * the app sent it and forwarded it verbatim. Doing the call here removes that hop entirely.
 *
 * Playback needs a **paid** anime365 subscription. Without one - or without a token at all - the
 * api answers with an error object and this produces no tracks, which leaves the web player as the
 * fallback.
 */
class Anime365ParserImpl @Inject constructor() : Anime365Parser {

    companion object {
        //the translation id is the tail of the embed url, and it is the id the api expects. The id
        //carried by the translation itself comes from Shikicinema and belongs to a different
        //namespace entirely - using it here is why this path never worked before.
        private val TRANSLATION_ID_REGEX = "translations/embed/(\\d+)".toRegex()
    }

    override fun video(video: TranslationVideo, tracks: List<Track>, subtitles: String?): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, subtitles, null)

    /**
     * Built from the embed link's own host, because anime365 serves the same content on four
     * domains and keeps moving between them - whichever one the link came from is the one that
     * will answer. Returns null when there is no token, since the call is pointless without one.
     */
    override fun apiUrl(playerUrl: String, token: String?): String? {
        if (token.isNullOrBlank()) return null

        val id = TRANSLATION_ID_REGEX.find(playerUrl)?.groupValues?.get(1) ?: return null
        val host = hostOf(playerUrl) ?: return null

        return "https://$host/api/translations/embed/$id?access_token=$token"
    }

    /**
     * One entry per quality, each carrying its own url list. Sound is muxed in, so no `audioUrl`.
     */
    override fun tracks(response: Anime365VideoResponse?): List<Track> {
        if (response == null || response.isError) return emptyList()

        return response.data?.stream
                .orEmpty()
                .mapNotNull { stream ->
                    val url = stream.urls?.firstOrNull { !it.isNullOrBlank() } ?: return@mapNotNull null
                    val quality = stream.height?.toString() ?: return@mapNotNull null

                    Track(quality, url)
                }
                .sortedByDescending { it.quality.toIntOrNull() ?: 0 }
    }

    /**
     * anime365 returns the subtitles path relative to its own host, so it has to be put back
     * together against the host the link came from.
     */
    override fun subtitles(response: Anime365VideoResponse?, playerUrl: String): String? {
        if (response == null || response.isError) return null

        val path = response.data?.subtitlesUrl
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http")) return path

        val host = hostOf(playerUrl) ?: return null
        return "https://$host${if (path.startsWith("/")) path else "/$path"}"
    }

    private fun hostOf(url: String): String? =
            Uri.parse(if (url.startsWith("//")) "https:$url" else url).host
}

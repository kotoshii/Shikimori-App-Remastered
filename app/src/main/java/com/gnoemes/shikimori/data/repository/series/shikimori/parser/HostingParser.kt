package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import io.reactivex.Single

/**
 * Turns a translation's player link into something the app can play, for one video hosting.
 *
 * Adding a hosting takes three things: a class in [VideoHosting] with its names in
 * `VideoHosting.fromName`, a parser implementing this, and one binding next to the others in
 * `SeriesUtilModule`. A hosting without a parser opens in the web player.
 */
interface HostingParser {

    /** The [VideoHosting] class this parser handles. */
    val hosting: Class<out VideoHosting>

    /**
     * Fetches the player page, and whatever it points to, and reads the streams out of it.
     *
     * A page with nothing playable on it is answered with [ParsedVideo.EMPTY], so the user is
     * told the video was not found. Network errors are passed on as they are - they have messages
     * of their own.
     */
    fun parse(playerUrl: String): Single<ParsedVideo>

    /** Headers the player and the downloader have to send along with the stream urls. */
    fun headers(video: Video): Map<String, String> = emptyMap()
}

/** What a parser found on the page: the tracks, and a subtitles url when the hosting has one. */
data class ParsedVideo(
        val tracks: List<Track>,
        val subtitles: String? = null
) {
    companion object {
        val EMPTY = ParsedVideo(emptyList())
    }
}

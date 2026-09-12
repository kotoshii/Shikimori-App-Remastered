package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.app.domain.exceptions.HostingChallengeException
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import javax.inject.Inject

class VkParserImpl @Inject constructor() : VkParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    override fun tracks(html: String?): List<Track> {
        if (html.isNullOrEmpty()) return emptyList()
        if (isChallenge(html)) throw HostingChallengeException()

        val regex = Regex("\"(mp4_144|mp4_240|mp4_360|mp4_480|mp4_720|mp4_1080)\":\\s?\"(.*?)\"")
        val matches = regex.findAll(html)

        return matches
                .map { it.destructured.toList() }
                .map {
                    val (key, value) = it
                    Track(key.replace("mp4_", ""), unescape(value))
                }
                .toList()
                .sortedByDescending { it.quality.toInt() }
    }

    /**
     * vk.com redirects to challenge.html - an "У вас большие запросы!" modal - when it decides a
     * request looks automated. That page carries no file list, so parsing it used to produce an
     * empty episode with nothing said about why.
     *
     * The marker is the query parameter the page's own script hashes. It is ascii, so it holds
     * whatever charset vk.com decides to serve the page with (the player page itself comes back as
     * windows-1251), and it appears nowhere in a real player page.
     */
    private fun isChallenge(html: String) = html.contains("hash429")

    /**
     * vk.com embeds the file list as json inside `window.cur`, so the urls arrive escaped:
     * `"mp4_480":"https:\/\/vkvd407.okcdn.ru\/?expires=..."`.
     *
     * Playback survived that - both ExoPlayer's [java.net.URL] and OkHttp's HttpUrl fold `\` into
     * `/` the way browsers do - but the same string is what long press copies to the clipboard and
     * what the downloader is handed, and neither should rest on two parsers being lenient.
     *
     * `\/` is the only escape vk.com actually emits today; the rest are here so that a json string
     * stays a json string rather than becoming a guess about one.
     */
    private fun unescape(value: String): String {
        if (!value.contains('\\')) return value

        val result = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val char = value[i]
            //a trailing backslash is not an escape, it is just the last character
            if (char != '\\' || i == value.lastIndex) {
                result.append(char)
                i++
                continue
            }

            when (val escaped = value[i + 1]) {
                'u' -> {
                    val hex = value.substring(i + 2, minOf(i + 6, value.length))
                    val code = if (hex.length == 4) hex.toIntOrNull(16) else null
                    if (code != null) {
                        result.append(code.toChar())
                        i += 6
                    } else {
                        //not a real \uXXXX, so leave the backslash where it was
                        result.append(char)
                        i++
                    }
                }
                'n' -> { result.append('\n'); i += 2 }
                'r' -> { result.append('\r'); i += 2 }
                't' -> { result.append('\t'); i += 2 }
                'b' -> { result.append('\b'); i += 2 }
                //covers \/ and \" as well as \\, which has to be consumed as a pair or it leaves a
                //stray backslash behind
                else -> { result.append(escaped); i += 2 }
            }
        }
        return result.toString()
    }
}

package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import android.os.Build
import androidx.annotation.RequiresApi
import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import io.lindstrom.m3u8.parser.MasterPlaylistParser
import io.reactivex.Single
import org.jsoup.Jsoup
import javax.inject.Inject

/**
 * Not registered in `SeriesUtilModule`, so SovetRomantica rows open in the web player: as of
 * 2026-10-02 sovetromantica.com and 24 of 25 sampled embeds redirected elsewhere, and its video
 * hosts were gone from DNS. Binding it like the other parsers brings it back.
 */
class SovetRomanticaParser @Inject constructor(
        private val api: VideoApi
) : HostingParser {

    override val hosting = VideoHosting.SOVET_ROMANTICA::class.java

    override fun parse(playerUrl: String): Single<ParsedVideo> {
        //the master playlist needs MasterPlaylistParser, which needs java 8 apis
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return Single.just(ParsedVideo.EMPTY)

        return api.getPlayerHtml(playerUrl)
                .flatMap { html ->
                    val playlistUrl = getMasterPlaylistUrl(html.string())
                            ?: return@flatMap Single.just(ParsedVideo.EMPTY)

                    api.getTextResponse(playlistUrl)
                            .map { ParsedVideo(tracks(it.string(), playlistUrl)) }
                }
    }

    override fun headers(video: Video): Map<String, String> = mapOf("Referer" to video.player)

    @RequiresApi(Build.VERSION_CODES.N)
    private fun tracks(m3uContent: String?, masterPlaylistUrl: String?): List<Track> {
        if (m3uContent == null || masterPlaylistUrl == null) return emptyList()

        val parser = MasterPlaylistParser()
        val playlist = parser.readPlaylist(m3uContent.replace("\r", ""))

        return playlist.variants()
                .map {
                    val quality = it.resolution().get().height().toString()
                    val url = masterPlaylistUrl.split("/").dropLast(1).plusElement(it.uri()).joinToString("/")

                    Track(quality, url)
                }
                .sortedByDescending { it.quality.toInt() }
    }

    private fun getMasterPlaylistUrl(html: String?): String? {
        if (html.isNullOrEmpty()) return null

        val regex = Regex("\"file\":\"+(.+?)\",")
        val scriptData = Jsoup.parse(html).select("script:containsData(file)").first()?.data() ?: return null

        return regex.find(scriptData)?.groupValues?.getOrNull(1)
    }
}
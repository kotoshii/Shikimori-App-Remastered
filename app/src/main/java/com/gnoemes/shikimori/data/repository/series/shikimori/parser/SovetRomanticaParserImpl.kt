package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import android.os.Build
import androidx.annotation.RequiresApi
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import io.lindstrom.m3u8.parser.MasterPlaylistParser
import org.jsoup.Jsoup
import javax.inject.Inject

class SovetRomanticaParserImpl @Inject constructor() : SovetRomanticaParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    @RequiresApi(Build.VERSION_CODES.N)
    override fun tracks(m3uContent: String?, masterPlaylistUrl: String?): List<Track> {
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

    override fun getMasterPlaylistUrl(html: String?): String? {
        if (html.isNullOrEmpty()) return null

        val regex = Regex("\"file\":\"+(.+?)\",")
        val scriptData = Jsoup.parse(html).select("script:containsData(file)").first()?.data() ?: return null

        return regex.find(scriptData)?.groupValues?.getOrNull(1)
    }
}
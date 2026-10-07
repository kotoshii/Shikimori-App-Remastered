package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import io.reactivex.Single
import org.jsoup.Jsoup
import javax.inject.Inject

class AllVideoParser @Inject constructor(
        private val api: VideoApi
) : HostingParser {

    override val hosting = VideoHosting.ALLVIDEO::class.java

    override fun parse(playerUrl: String): Single<ParsedVideo> =
            api.getPlayerHtml(playerUrl).map { ParsedVideo(tracks(it.string())) }

    private fun tracks(html: String?): List<Track> {
        if (html.isNullOrEmpty()) return emptyList()

        val doc = Jsoup.parse(html)
        val scriptData = doc.select("script:containsData(isMobile):containsData(file:)").first()?.data()

        return scriptData
                ?.substringAfter("file:\"")
                ?.substringBefore('"')
                ?.split(",")
                .orEmpty()
                .map {
                    val match = Regex("\\[(\\d+)p\\](.+)").find(it)

                    if (match == null) {
                        Track("unknown", it)
                    } else {
                        val (quality, url) = match.destructured
                        Track(quality, url)
                    }
                }
                .sortedByDescending { it.quality.toInt() }
    }
}
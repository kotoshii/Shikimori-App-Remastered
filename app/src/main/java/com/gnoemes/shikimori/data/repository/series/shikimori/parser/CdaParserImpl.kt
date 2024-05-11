package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.entity.series.data.CdaApiRequest
import com.gnoemes.shikimori.entity.series.data.CdaPlayerData
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.google.gson.Gson
import io.reactivex.Single
import org.jsoup.Jsoup
import javax.inject.Inject

class CdaParserImpl @Inject constructor(
        private val api: VideoApi
) : CdaParser {

    override fun video(video: TranslationVideo, tracks: List<Track>): Video =
            Video(video.animeId, video.episodeIndex.toLong(), video.webPlayerUrl!!, video.videoHosting, tracks, null, null)

    override fun tracks(videoLinkPairs: List<Pair<String, String?>>): List<Track> {
        return videoLinkPairs
                .mapNotNull {
                    if (it.second == null) return@mapNotNull null

                    val quality = getResolution(it.first) ?: return@mapNotNull null
                    Track(quality, it.second!!)
                }
                .sortedByDescending { it.quality.toInt() }
    }

    override fun parsePlayerData(html: String?): CdaPlayerData? {
        if (html.isNullOrEmpty()) return null

        val doc = Jsoup.parse(html)
        val playerDataJson = doc.select(".brdPlayer > div").first().attr("player_data")

        return Gson().fromJson<CdaPlayerData>(playerDataJson, CdaPlayerData::class.java)
    }

    override fun getVideoLinks(playerData: CdaPlayerData?): Single<List<Pair<String, String?>>> {
        if (playerData == null) return Single.just(emptyList())

        val (id, _, ts, hash2) = playerData.video

        return Single.fromCallable { playerData.video.cdaQualities }
                .flatMap {
                    Single.merge(
                            it.map { quality -> getVideoLink(id, quality, ts, hash2)
                                    .map { response -> Pair(quality, response.body()?.result?.resp) }
                            }
                    ).toList()
                }
    }

    private fun getVideoLink(videoId: String, quality: String, ts: Long, hash2: String) =
            api.cdaApiRequest(
                    CdaApiRequest(
                            1,
                            "2.0",
                            "videoGetLink",
                            listOf(
                                    videoId,
                                    quality,
                                    ts,
                                    hash2
                            )
                    )
            )

    private fun getResolution(cdaQuality: String): String? {
        return when (cdaQuality) {
            "vl" -> "360"
            "lq" -> "480"
            "sd" -> "720"
            "hd" -> "1080"
            else -> null
        }
    }
}
package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.data.CdaPlayerData
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import io.reactivex.Single

interface CdaParser {

    fun video(video: TranslationVideo, tracks: List<Track>): Video

    fun tracks(videoLinkPairs: List<Pair<String, String?>>): List<Track>

    fun parsePlayerData(html: String?): CdaPlayerData?

    fun getVideoLinks(playerData: CdaPlayerData?): Single<List<Pair<String, String?>>>
}
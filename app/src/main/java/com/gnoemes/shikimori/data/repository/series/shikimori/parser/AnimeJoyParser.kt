package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import com.gnoemes.shikimori.utils.toUri
import io.reactivex.Single
import javax.inject.Inject

class AnimeJoyParser @Inject constructor() : HostingParser {

    override val hosting = VideoHosting.ANIMEJOY::class.java

    //the qualities are in the embed link itself, so there is no page to fetch
    override fun parse(playerUrl: String): Single<ParsedVideo> =
            Single.fromCallable { ParsedVideo(playerJsTracks(playerUrl.toUri().getQueryParameter("file"))) }
}
package com.gnoemes.shikimori.data.repository.series.shikimori.converter

import com.gnoemes.shikimori.entity.series.data.EpisodeResponse
import com.gnoemes.shikimori.entity.series.domain.Episode

interface  EpisodeResponseConverter {

    fun convertResponse(it : EpisodeResponse) : Episode
}
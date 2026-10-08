package com.gnoemes.shikimori.data.repository.series.shikimori.converter

import com.gnoemes.shikimori.entity.series.data.EpisodeResponse
import com.gnoemes.shikimori.entity.series.domain.Episode
import javax.inject.Inject

class EpisodeResponseConverterImpl @Inject constructor(): EpisodeResponseConverter {

    override fun convertResponse(it : EpisodeResponse) : Episode {
        return Episode(
                it.id,
                it.index,
                it.animeId
        )
    }
}
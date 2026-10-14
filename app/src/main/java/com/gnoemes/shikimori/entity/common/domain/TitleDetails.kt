package com.gnoemes.shikimori.entity.common.domain

import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.entity.user.domain.Statistic
import org.joda.time.DateTime

/** What the anime and the manga page share of a title's details. */
interface TitleDetails {
    val id: Long
    val name: String
    val nameRu: String?
    val image: Image
    val url: String
    val status: Status
    val dateAired: DateTime?
    val score: Double
    val description: String?
    val topicId: Long?
    val genres: List<GenreV2>
    val userRate: UserRate?
    val rateScoresStats: List<Statistic>
    val rateStatusesStats: List<Statistic>
}

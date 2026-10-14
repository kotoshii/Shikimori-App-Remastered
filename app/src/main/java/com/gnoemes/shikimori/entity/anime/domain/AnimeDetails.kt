package com.gnoemes.shikimori.entity.anime.domain

import com.gnoemes.shikimori.entity.common.domain.AgeRating
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.common.domain.TitleDetails
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.entity.studio.Studio
import com.gnoemes.shikimori.entity.user.domain.Statistic
import org.joda.time.DateTime

data class AnimeDetails(
        override val id: Long,
        override val name: String,
        override val nameRu: String?,
        override val image: Image,
        override val url: String,
        val type: AnimeType,
        override val status: Status,
        val episodes: Int,
        val episodesAired: Int,
        override val dateAired: DateTime?,
        val dateReleased: DateTime?,
        val nextEpisodeDate: DateTime?,
        val namesEnglish: List<String?>?,
        val namesJapanese: List<String?>?,
        val ageRating: AgeRating,
        override val score: Double,
        val duration: Int,
        override val description: String?,
        val descriptionHtml: String,
        val franchise : String?,
        val favoured: Boolean,
        override val topicId: Long?,
        override val genres: List<GenreV2>,
        override val userRate: UserRate?,
        val videos: List<AnimeVideo>?,
        val studios: List<Studio>,
        override val rateScoresStats: List<Statistic>,
        override val rateStatusesStats: List<Statistic>
) : TitleDetails
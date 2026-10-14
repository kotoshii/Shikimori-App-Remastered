package com.gnoemes.shikimori.entity.manga.domain

import com.gnoemes.shikimori.entity.common.domain.GenreV2
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.common.domain.TitleDetails
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.entity.user.domain.Statistic
import org.joda.time.DateTime

data class MangaDetails(
        override val id: Long,
        override val name: String,
        override val nameRu: String?,
        override val image: Image,
        override val url: String,
        val type: MangaType,
        override val status: Status,
        val volumes: Int,
        val chapters: Int,
        override val dateAired: DateTime?,
        val dateReleased: DateTime?,
        override val score: Double,
        override val description: String?,
        val descriptionHtml: String,
        val franchise : String?,
        val favoured: Boolean,
        override val topicId: Long?,
        override val genres: List<GenreV2>,
        override val userRate: UserRate?,
        override val rateScoresStats: List<Statistic>,
        override val rateStatusesStats: List<Statistic>
) : TitleDetails
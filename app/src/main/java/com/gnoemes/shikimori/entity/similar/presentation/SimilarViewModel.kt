package com.gnoemes.shikimori.entity.similar.presentation

import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.RateButtonListItem
import com.gnoemes.shikimori.entity.rates.domain.RateStatus

data class SimilarViewModel(
        override val id: Long,
        override val rateId : Long,
        override val type: Type,
        override val title: String,
        val description: CharSequence,
        val image: Image,
        val score : Double?,
        override val status: RateStatus?,
        val isGuest : Boolean
) : RateButtonListItem
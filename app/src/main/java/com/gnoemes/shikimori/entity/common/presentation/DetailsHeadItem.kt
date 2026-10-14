package com.gnoemes.shikimori.entity.common.presentation

import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.rates.domain.RateStatus

data class DetailsHeadItem(
        val name: String,
        val image: Image,
        val score: Double,
        val rateStatus: RateStatus?,
        val isGuest : Boolean
)
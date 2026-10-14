package com.gnoemes.shikimori.presentation.presenter.rates.provider

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.RateStatus

interface RateResourceProvider {

    fun getRateStatusName(status: RateStatus, isAnime: Boolean): String

    fun getChangeRateStatusMessage(type : Type, status : RateStatus) : String

    fun getDeleteRateMessage(type : Type) : String
}
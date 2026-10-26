package com.gnoemes.shikimori.entity.rates.presentation

import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.Rate

data class RateViewModel(
        val id: Long,
        val contentId: Long,
        val type: Type,
        val image: Image,
        val name: String,
        val description: CharSequence,
        val rating: String,
        val progress: String,
        val rawRate: Rate,
        val isPinned : Boolean,
        val pinOrder : Int,
        /** False on another user's list: shikimori refuses every change to it (BUGS #85). */
        val canEdit : Boolean
)
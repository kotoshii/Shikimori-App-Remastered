package com.gnoemes.shikimori.entity.series.presentation


data class TranslationViewModel(
        val firstVideoId : Long,
        val authors: String,
        val description: CharSequence?,
        val videos: List<TranslationVideo>,
        val isSameAuthor: Boolean,
        val canBeDownloaded : Boolean,
        val episodesSize : Int
)
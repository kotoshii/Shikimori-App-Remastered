package com.gnoemes.shikimori.entity.topic.presentation

import com.gnoemes.shikimori.entity.common.domain.Type

/**
 * The block about what a topic is on: an anime, a manga or a ranobe with its type, season and status
 * lines; a character, a person or a club with its name only.
 */
data class TopicLinkedViewModel(
        val type: Type,
        val id: Long,
        val imageUrl: String?,
        val name: String,
        val typeText: CharSequence? = null,
        val seasonText: CharSequence? = null,
        val statusText: CharSequence? = null
)

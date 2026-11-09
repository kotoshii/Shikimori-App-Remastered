package com.gnoemes.shikimori.entity.topic.presentation

import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText

data class TopicContentViewModel(
        val title : String,
        val content : RichText
)

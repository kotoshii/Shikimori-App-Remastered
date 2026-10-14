package com.gnoemes.shikimori.presentation.presenter.topic.provider

import com.gnoemes.shikimori.entity.forum.domain.ForumType

interface TopicResourceProvider {

    fun getTopicName(type : ForumType) : String

    fun getCommentsMoreText(limit: Int, diff: Int?): String
}
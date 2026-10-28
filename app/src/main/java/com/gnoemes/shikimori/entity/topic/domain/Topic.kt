package com.gnoemes.shikimori.entity.topic.domain

import com.gnoemes.shikimori.entity.common.domain.LinkedContent
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.user.domain.UserBrief
import org.joda.time.DateTime

data class Topic(
        val id: Long,
        val title: String,
        val description: String?,
        val descriptionHtml: String?,
        val footer: String?,
        val dateCreated: DateTime,
        val commentsCount: Long,
        //the api's own name: a topic needs no forum type, so one in a forum the app does not know opens
        //too (BUGS #41)
        val forumName: String,
        val user: UserBrief,
        val type: TopicType,
        val linkedType: Type,
        val linked: LinkedContent?,
        val isViewed: Boolean,
        val event : TopicEvent,
        val episode : String?
)
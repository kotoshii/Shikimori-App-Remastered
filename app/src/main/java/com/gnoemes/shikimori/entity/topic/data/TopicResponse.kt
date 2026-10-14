package com.gnoemes.shikimori.entity.topic.data

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.forum.data.ForumResponse
import com.gnoemes.shikimori.entity.topic.domain.TopicEvent
import com.gnoemes.shikimori.entity.user.data.UserBriefResponse
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.joda.time.DateTime

/**
 * Read by Gson as it is. [linked] stays raw JSON: its class depends on [linkedType], which Gson cannot
 * look at while reading it, so the converter reads it once the kind is known.
 */
data class TopicResponse(
        @field:SerializedName("id") val id: Long,
        //null for a tierlist's topic (seen 2026-10-14)
        @field:SerializedName("topic_title") val title: String?,
        @field:SerializedName("body") val description: String?,
        @field:SerializedName("html_body") val descriptionHtml: String?,
        @field:SerializedName("html_footer") val footer: String?,
        @field:SerializedName("created_at") val dateCreated: DateTime,
        @field:SerializedName("comments_count") val commentsCount: Long,
        @field:SerializedName("forum") val forum: ForumResponse,
        @field:SerializedName("user") val user: UserBriefResponse,
        @field:SerializedName("type") val type: String?,
        @field:SerializedName("linked_type") private val _linkedType: Type?,
        @field:SerializedName("linked") val linked: JsonElement?,
        @field:SerializedName("viewed") val isViewed: Boolean,
        @field:SerializedName("event") private val _event: TopicEvent?,
        @field:SerializedName("episode") val episode: String?
) {
    val linkedType: Type
        get() = _linkedType ?: Type.UNKNOWN

    //no event, or one the app does not know
    val event: TopicEvent
        get() = _event ?: TopicEvent.NONE
}
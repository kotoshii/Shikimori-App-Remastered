package com.gnoemes.shikimori.presentation.presenter.topic.provider

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.forum.domain.ForumType
import com.gnoemes.shikimori.utils.nameRes
import javax.inject.Inject

class TopicResourceProviderImpl @Inject constructor(
        private val context: Context
) : TopicResourceProvider {

    override fun getTopicName(type: ForumType): String = context.getString(type.nameRes)

    override fun getCommentsMoreText(limit: Int, diff: Int?): String {
        return if (diff == null) {
            context.getString(R.string.comment_more_format_message_last) + " " + context.resources.getQuantityString(R.plurals.commentaries, limit, limit)
        } else {
            val firstPart = String.format(context.getString(R.string.comment_more_format_message), limit)
            firstPart + " " + context.resources.getQuantityString(R.plurals.commentaries, diff, diff)
        }
    }
}
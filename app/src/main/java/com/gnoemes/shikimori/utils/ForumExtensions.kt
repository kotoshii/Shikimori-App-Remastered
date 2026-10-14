package com.gnoemes.shikimori.utils

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.forum.domain.ForumType
import com.gnoemes.shikimori.entity.topic.domain.TopicType

/** A forum's name, in the forum menu and on a topic's toolbar alike. */
@get:StringRes
val ForumType.nameRes: Int
    get() = when (this) {
        ForumType.ALL -> R.string.forum_all
        ForumType.MY_CLUBS -> R.string.forum_my_clubs
        ForumType.NEWS -> R.string.forum_news
        ForumType.ANIME_AND_MANGA -> R.string.forum_animanga
        ForumType.CLUBS -> R.string.forum_clubs
        ForumType.COLLECTIONS -> R.string.forum_collection
        ForumType.CONTESTS -> R.string.forum_contests
        ForumType.COSPLAY -> R.string.forum_cosplay
        ForumType.GAMES -> R.string.forum_games
        ForumType.OFF_TOPIC -> R.string.forum_off_topic
        ForumType.REVIEWS -> R.string.forum_reviews
        ForumType.SITE -> R.string.forum_site
        ForumType.VISUAL_NOVELS -> R.string.forum_vn
        ForumType.ARTICLES -> R.string.forum_articles
        ForumType.TIERLISTS -> R.string.forum_tierlists
        ForumType.CRITIQUES -> R.string.forum_critiques
        ForumType.HIDDEN -> R.string.forum_hidden
        ForumType.PREMODERATION -> R.string.forum_premoderation
    }

/** A topic's tag by its kind: the text and its colour together. */
class TopicTag(@StringRes val text: Int, @ColorRes val color: Int)

/** «новость», «коллекция»…; null for a kind without a tag. */
val TopicType.tag: TopicTag?
    get() = when (this) {
        TopicType.NEWS, TopicType.NEWS_LINK_ONLY -> TopicTag(R.string.topic_tag_news, R.color.topic_news)
        TopicType.COLLECTION -> TopicTag(R.string.topic_tag_collection, R.color.topic_collection)
        TopicType.COSPLAY -> TopicTag(R.string.topic_tag_cosplay, R.color.topic_cosplay)
        TopicType.REVIEW -> TopicTag(R.string.topic_tag_review, R.color.topic_review)
        else -> null
    }

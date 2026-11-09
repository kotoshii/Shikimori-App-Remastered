package com.gnoemes.shikimori.entity.topic.presentation

import com.gnoemes.shikimori.entity.common.presentation.richtext.RichText

/*
 * The topic page is one list (P8-T4, the one-list step): TopicPresenter builds its rows, TopicAdapter shows
 * them. Besides these, the rows are the post's author (TopicUserViewModel), the title block
 * (TopicLinkedViewModel), the comments (CommentViewModel), a spinner (ProgressItem) and «Комментарии не
 * найдены.» (PlaceholderItem).
 */

/** The post's title. */
data class TopicTitleRow(val title: String)

/**
 * One top-level part of the post: [index] in [text]'s blocks. A long post is built part by part as it
 * scrolls in (P8-T4 Q29: «Архитектура в Аниме» has 201 parts); the whole text comes along for what spans
 * the parts (the zoom gallery, what is opened).
 */
data class TopicPostPartRow(val text: RichText, val index: Int)

/** The space and the line under the post. */
object TopicPostEndRow

/** «Комментарии (N):», with the post's total. */
data class CommentsHeaderRow(val count: Long)

/** «Загрузить еще …»: the next page of older comments. */
data class CommentsMoreRow(val text: String)

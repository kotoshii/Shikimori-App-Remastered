package com.gnoemes.shikimori.data.repository.topic

import com.gnoemes.shikimori.entity.forum.domain.Forum
import com.gnoemes.shikimori.entity.forum.domain.ForumType
import com.gnoemes.shikimori.entity.topic.domain.Topic
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Maybe
import io.reactivex.Single

interface TopicRepository {

    fun getList(page: Int, limit: Int, type: ForumType): Single<Page<Topic>>

    fun getDetails(id: Long): Single<Topic>

    /**
     * The topic of an entry that has one (`linkedType` as the api names it: `Collection`, `Article`, `Critique`,
     * `Review`); empty when the api gives none, as for an 18+ topic the user hides.
     */
    fun getLinkedTopicId(linkedType: String, linkedId: Long): Maybe<Long>

    fun getForums(): Single<List<Forum>>
}
package com.gnoemes.shikimori.domain.common

import com.gnoemes.shikimori.data.repository.topic.TopicRepository
import com.gnoemes.shikimori.data.repository.user.UserRepository
import com.gnoemes.shikimori.entity.common.presentation.richtext.TopicEntry
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject

/**
 * The id a link in user text needs to open an app screen, when the HTML does not carry it (BB_CODES.md §4). A
 * helper with an `@Inject` constructor rather than an interactor method: every presenter has it
 * (`BaseNetworkPresenter`), so both repositories are bound for the whole app, while the interactors are bound
 * only in some screens' modules. Until P8-T4's Q38 it was `UserLookup`, with the nickname lookup alone.
 */
class LinkLookup @Inject constructor(
        private val userRepository: UserRepository,
        private val topicRepository: TopicRepository
) {

    /** A user known only by the nickname: an avatar link (`[user]nick[/user]`), a quote's or a ban's author. */
    fun userId(nickname: String): Single<Long> =
            userRepository.getUserIdByNickname(nickname)
                    .applyErrorHandlerAndSchedulers()

    /** The topic of a collection, an article, a critique or a review; empty when the api gives none. */
    fun topicId(entry: TopicEntry, id: Long): Maybe<Long> =
            topicRepository.getLinkedTopicId(entry.linkedType, id)
                    .applyErrorHandlerAndSchedulers()
}

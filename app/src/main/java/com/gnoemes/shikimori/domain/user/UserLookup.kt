package com.gnoemes.shikimori.domain.user

import com.gnoemes.shikimori.data.repository.user.UserRepository
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Single
import javax.inject.Inject

/**
 * A user known only by the nickname — an avatar link in user text (`[user]nick[/user]`, a quote's or a
 * ban's author carries no id, BB_CODES.md §4) — to their id. A helper with an `@Inject` constructor rather
 * than a `UserInteractor` method: every presenter has it (`BaseNetworkPresenter`), and the user repository
 * is bound for the whole app, while `UserInteractor` is bound only in some screens' modules.
 */
class UserLookup @Inject constructor(
        private val repository: UserRepository
) {

    fun idByNickname(nickname: String): Single<Long> =
            repository.getUserIdByNickname(nickname)
                    .applyErrorHandlerAndSchedulers()
}

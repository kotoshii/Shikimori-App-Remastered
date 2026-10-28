package com.gnoemes.shikimori.data.local.preference

import com.gnoemes.shikimori.entity.user.domain.UserBrief
import com.gnoemes.shikimori.entity.user.domain.UserStatus

interface UserSource {

    fun getUserId(): Long

    /**
     * Signed in to shikimori: the one rule, wherever the app asks - the status sign-in saves as its last
     * step, once the user has loaded. A third of the checks read the saved id instead, which sign-in
     * saves a moment earlier (P7-T5 17.9).
     */
    fun isSignedIn(): Boolean

    fun setUserId(id: Long)

    fun getUser(): UserBrief?

    fun setUser(user: UserBrief)

    fun clearUser()

    fun getUserStatus(): UserStatus

    fun setUserStatus(status: UserStatus)
}
package com.gnoemes.shikimori.entity.common.presentation.richtext

import com.gnoemes.shikimori.entity.common.domain.Type

/** What a tap on a link in user-written text opens. */
sealed class LinkTarget {

    /**
     * An app screen by id: `ANIME`, `MANGA`, `RANOBE`, `CHARACTER`, `PERSON`, `USER`, `TOPIC`, or a
     * `COMMENT`, which the app shows in a sheet.
     */
    data class Screen(val type: Type, val id: Long) : LinkTarget()

    /** A user known only by the nickname (`[user]nick[/user]`, a quote's or a ban's author): one lookup first. */
    data class UserByNickname(val nickname: String) : LinkTarget()

    /** Any other page, opened outside the app. */
    data class Web(val url: String) : LinkTarget()
}

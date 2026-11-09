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

    /**
     * A studio's page (`/animes/studio/<id>`): the catalog filtered by it, as the title page's studio row
     * opens it (the user, 2026-11-09, P8-T4 Q17). [name] is the link's text, when that is a name.
     */
    data class Studio(val id: Long, val name: String?) : LinkTarget()

    /** Any other page, opened outside the app. */
    data class Web(val url: String) : LinkTarget()
}

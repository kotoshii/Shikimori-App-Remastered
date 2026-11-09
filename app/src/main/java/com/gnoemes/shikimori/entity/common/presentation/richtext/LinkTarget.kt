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

    /**
     * The page of an entry that is a topic on the site: a collection, an article, a critique, a review. The site
     * builds that page from the entry's topic, as the forum's topic page and the api do (BB_CODES.md §4), so it
     * opens the app's topic page; the link names the entry, so its topic is looked up first. [url] is the
     * link's own address, for the browser when the api has no topic for it (an 18+ topic the user hides, a
     * draft).
     */
    data class EntryTopic(val entry: TopicEntry, val id: Long, val url: String) : LinkTarget()

    /** Any other page, opened outside the app. */
    data class Web(val url: String) : LinkTarget()
}

/** The entries whose page is their topic; [linkedType] is how the api names them (`linked_type`). */
enum class TopicEntry(val linkedType: String) {
    COLLECTION("Collection"),
    ARTICLE("Article"),
    CRITIQUE("Critique"),
    REVIEW("Review")
}

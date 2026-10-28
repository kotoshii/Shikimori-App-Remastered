package com.gnoemes.shikimori.entity.common.domain

import com.gnoemes.shikimori.utils.dropExtraPageItem

/**
 * A page of a paged list, and whether it is the last: the paginator ends the list there instead of
 * asking for one more page that comes back empty (P7-T5 17.4). How a page knows depends on what shikimori
 * sends for that list, so each list's repository builds its pages with the factory that fits it - from
 * the rows as they came, before anything is dropped from them.
 */
class Page<T>(val items: List<T>, val isLast: Boolean) {

    fun <R> map(transform: (List<T>) -> List<R>): Page<R> = Page(transform(items), isLast)

    companion object {

        /** Rates, history, comments: shikimori sends one row more than [limit] while a next page exists. */
        fun <T> withExtraRow(rows: List<T>, limit: Int): Page<T> = Page(rows.dropExtraPageItem(limit), rows.size <= limit)

        /** The catalog, friends: exactly [limit] rows a page, so a shorter one is the last. */
        fun <T> exact(rows: List<T>, limit: Int): Page<T> = Page(rows, rows.size < limit)

        /**
         * Topics: one row more than [limit] too, but the topics the reader may not see are dropped after
         * the page is taken, so a short page proves nothing and only an empty one ends the list.
         */
        fun <T> endsWhenEmpty(rows: List<T>, limit: Int): Page<T> = Page(rows.dropExtraPageItem(limit), rows.isEmpty())
    }
}
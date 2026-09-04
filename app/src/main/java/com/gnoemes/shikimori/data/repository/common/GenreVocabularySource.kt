package com.gnoemes.shikimori.data.repository.common

import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import io.reactivex.Completable

/**
 * Every genre the app knows about, for the catalog filter to offer.
 *
 * Backed by a **local list that only ever grows**. [refresh] merges what shikimori currently
 * publishes into it: new genres are added, known ones have their labels updated, and genres missing
 * from the answer are **kept**.
 *
 * That is deliberate. Shikimori's handling of hentai, yaoi, yuri, shoujo-ai / shounen-ai and erotica
 * has been unstable under russian "lgbt propaganda" law - titles come and go on the website - and a
 * plain fetch-and-replace would erase those genres from the app the first time the api answered
 * without them, with no way for the user to get them back. Accumulating means the app keeps what it
 * has already seen.
 */
interface GenreVocabularySource {

    /**
     * The stored vocabulary, ordered demographic -> genre -> theme and alphabetically inside each
     * kind. Never empty: a bundled snapshot seeds the store on first use, so the filter screen works
     * before any network call and during an outage.
     */
    fun genres(type: GenreEntryType): List<GenreV2>

    /** Fetches both vocabularies and merges them in. Never fails - see the accumulate rule. */
    fun refresh(): Completable
}

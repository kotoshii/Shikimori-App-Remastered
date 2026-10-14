package com.gnoemes.shikimori.data.repository.common

import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import io.reactivex.Single
import io.reactivex.functions.BiFunction

interface TitleGenreSource {

    /**
     * v2 genres of one title, in the order shikimori returns them.
     *
     * Never fails: the rest details call is what draws the screen, and genres are an addition to
     * it, so anything going wrong here yields an empty list instead of an error the details
     * presenter would show.
     */
    fun genres(type: GenreEntryType, id: Long): Single<List<GenreV2>>

    /**
     * [details] with the title's genres put in by [attach]. Genres come from graphql rather than from
     * the rest response: only graphql carries the v2 taxonomy at all, and the rest response's own
     * `genres` array is empty for titles added from 2025 onward. [genres] never fails, so the details
     * still load when it comes back empty - see docs/_internal/GENRES_V2_SPIKE.md.
     */
    fun <T> withGenres(details: Single<T>, type: GenreEntryType, id: Long, attach: (T, List<GenreV2>) -> T): Single<T> =
            Single.zip(details, genres(type, id), BiFunction { loaded: T, genres: List<GenreV2> -> attach(loaded, genres) })
}

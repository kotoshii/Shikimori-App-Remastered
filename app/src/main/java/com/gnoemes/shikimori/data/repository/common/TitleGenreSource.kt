package com.gnoemes.shikimori.data.repository.common

import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import io.reactivex.Single

interface TitleGenreSource {

    /**
     * v2 genres of one title, in the order shikimori returns them.
     *
     * Never fails: the rest details call is what draws the screen, and genres are an addition to
     * it, so anything going wrong here yields an empty list instead of an error the details
     * presenter would show.
     */
    fun genres(type: GenreEntryType, id: Long): Single<List<GenreV2>>
}

package com.gnoemes.shikimori.data.repository.common

import com.gnoemes.shikimori.entity.common.data.graphql.PosterKey
import com.gnoemes.shikimori.entity.common.data.graphql.PosterResponse

interface PosterSource {

    /**
     * Resolves real poster urls for content whose REST image is a `missing_*` placeholder.
     *
     * Blocking - meant to be called from a background thread. Keys without a poster (or that
     * could not be fetched) are simply absent from the result, never throwing.
     */
    fun resolve(keys: Collection<PosterKey>): Map<PosterKey, PosterResponse>

    /**
     * Tells the source that shikimori is throttling us. Poster lookups are a nice-to-have, so
     * they must be the first thing to give way when the api quota runs out.
     */
    fun onRateLimited()
}

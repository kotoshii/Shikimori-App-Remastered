package com.gnoemes.shikimori.data.network

import com.gnoemes.shikimori.entity.common.data.graphql.GenreQueryResponse
import com.gnoemes.shikimori.entity.common.data.graphql.GenreVocabularyResponse
import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlRequest
import com.gnoemes.shikimori.entity.common.data.graphql.PosterQueryResponse
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface GraphqlApi {

    /**
     * Returns a [Call] rather than a `Single` on purpose: the only caller is an OkHttp
     * interceptor, which is already on a background thread and needs a blocking result.
     */
    @POST("/api/graphql")
    fun getPosters(@Body request: GraphqlRequest): Call<PosterQueryResponse>

    /**
     * v2 genres of a title. A `Single` here, unlike [getPosters] - this one is called from a
     * repository and composed with the rest details call.
     */
    @POST("/api/graphql")
    fun getGenres(@Body request: GraphqlRequest): Single<GenreQueryResponse>

    /**
     * Every genre shikimori publishes, both entry types at once. Feeds the catalog filter, see
     * `GenreVocabularySource`.
     */
    @POST("/api/graphql")
    fun getGenreVocabulary(@Body request: GraphqlRequest): Single<GenreVocabularyResponse>
}

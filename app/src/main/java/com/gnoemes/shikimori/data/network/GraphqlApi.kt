package com.gnoemes.shikimori.data.network

import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlRequest
import com.gnoemes.shikimori.entity.common.data.graphql.PosterQueryResponse
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
}

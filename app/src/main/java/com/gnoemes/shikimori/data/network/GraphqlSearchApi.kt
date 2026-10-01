package com.gnoemes.shikimori.data.network

import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlRequest
import com.gnoemes.shikimori.entity.common.data.graphql.SearchQueryResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * The catalog search. A separate interface from [GraphqlApi] rather than the same one on another
 * retrofit, so which client is in play is visible at the injection point: this one carries the
 * user's token, [GraphqlApi] does not.
 */
interface GraphqlSearchApi {

    @POST("/api/graphql")
    fun search(@Body request: GraphqlRequest): Single<SearchQueryResponse>
}

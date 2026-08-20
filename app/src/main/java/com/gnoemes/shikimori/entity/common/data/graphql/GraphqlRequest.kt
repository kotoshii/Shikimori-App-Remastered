package com.gnoemes.shikimori.entity.common.data.graphql

import com.google.gson.annotations.SerializedName

/**
 * Body of a Shikimori GraphQL call.
 *
 * The endpoint (`/api/graphql`) accepts a plain JSON object, so no GraphQL client library
 * is required - the regular Retrofit/Gson stack is enough.
 */
data class GraphqlRequest(
        @field:SerializedName("query") val query: String
)

package com.gnoemes.shikimori.data.network

import com.gnoemes.shikimori.entity.common.data.FranchiseResponse
import com.gnoemes.shikimori.entity.common.data.LinkResponse
import com.gnoemes.shikimori.entity.common.data.RelatedResponse
import com.gnoemes.shikimori.entity.common.data.RolesResponse
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.data.MangaDetailsResponse
import com.gnoemes.shikimori.entity.manga.data.MangaResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

/**
 * Manga and ranobe: the same api under two addresses, `/api/mangas` and `/api/ranobe`. [kind] is the
 * one place that picks the address for a [Type].
 */
interface MangaApi {

    companion object {
        fun kind(type: Type): String = if (type == Type.RANOBE) "ranobe" else "mangas"
    }

    @GET("/api/{kind}")
    fun getList(@Path("kind") kind: String, @QueryMap(encoded = true) filter: Map<String, String>): Single<List<MangaResponse>>

    @GET("/api/{kind}/{id}")
    fun getDetails(@Path("kind") kind: String, @Path("id") id: Long): Single<MangaDetailsResponse>

    @GET("/api/{kind}/{id}/roles")
    fun getRoles(@Path("kind") kind: String, @Path("id") id: Long): Single<List<RolesResponse>>

    @GET("/api/{kind}/{id}/similar")
    fun getSimilar(@Path("kind") kind: String, @Path("id") id: Long): Single<List<MangaResponse>>

    @GET("/api/{kind}/{id}/related")
    fun getRelated(@Path("kind") kind: String, @Path("id") id: Long): Single<List<RelatedResponse>>

    @GET("/api/{kind}/{id}/franchise")
    fun getFranchise(@Path("kind") kind: String, @Path("id") id: Long): Single<FranchiseResponse>

    @GET("/api/{kind}/{id}/external_links")
    fun getLinks(@Path("kind") kind: String, @Path("id") id: Long): Single<List<LinkResponse>>
}

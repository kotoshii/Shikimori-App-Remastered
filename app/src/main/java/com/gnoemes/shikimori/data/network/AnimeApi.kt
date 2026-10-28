package com.gnoemes.shikimori.data.network

import com.gnoemes.shikimori.entity.anime.data.AnimeDetailsResponse
import com.gnoemes.shikimori.entity.anime.data.AnimeResponse
import com.gnoemes.shikimori.entity.anime.data.ScreenshotResponse
import com.gnoemes.shikimori.entity.common.data.FranchiseResponse
import com.gnoemes.shikimori.entity.common.data.LinkResponse
import com.gnoemes.shikimori.entity.common.data.RelatedResponse
import com.gnoemes.shikimori.entity.common.data.RolesResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface AnimeApi {

    //Retrofit encodes the values: they went out as already encoded, and nothing encoded them, so a typed `+`
    //reached shikimori as a space and a `%` made it answer 400 (P7-T5 17.7, BUGS #90)
    @GET("/api/animes")
    fun getList(@QueryMap filters: Map<String, String>): Single<MutableList<AnimeResponse>>

    @GET("/api/animes/{id}")
    fun getDetails(@Path("id") id : Long) : Single<AnimeDetailsResponse>

    @GET("/api/animes/{id}/external_links")
    fun getLinks(@Path("id") id: Long): Single<List<LinkResponse>>

    @GET("/api/animes/{id}/similar")
    fun getSimilar(@Path("id") id: Long): Single<List<AnimeResponse>>

    @GET("/api/animes/{id}/related")
    fun getRelated(@Path("id") id: Long): Single<List<RelatedResponse>>

    @GET("/api/animes/{id}/franchise")
    fun getFranchise(@Path("id") id: Long): Single<FranchiseResponse>

    @GET("/api/animes/{id}/roles")
    fun getRoles(@Path("id") animeId: Long): Single<List<RolesResponse>>

    @GET("/api/animes/{id}/screenshots")
    fun getScreenshots(@Path("id") animeId: Long): Single<List<ScreenshotResponse>>
}
package com.gnoemes.shikimori.data.network

import com.gnoemes.shikimori.entity.series.data.*
import com.gnoemes.shikimori.entity.series.data.anime365.Anime365VideoResponse
import com.gnoemes.shikimori.entity.series.data.kodik.KodikLinksResponse
import com.gnoemes.shikimori.entity.series.data.kodik.KodikSearchResponse
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface VideoApi {

    @GET("/api/anime/{id}/series")
    fun getEpisodes(@Path("id") id: Long): Single<List<EpisodeResponse>>

    @GET("/api/anime/alternative/{id}/series")
    fun getEpisodesAlternative(@Path("id") id: Long): Single<List<EpisodeResponse>>

    @GET("/api/anime/{animeId}/{episodeId}/translations")
    fun getTranslations(@Path("animeId") animeId: Long,
                        @Path("episodeId") episodeId: Long,
                        @Query("type") type: String
    ): Single<List<TranslationResponse>>

    @GET("/api/anime/alternative/{animeId}/{episodeId}/translations")
    fun getTranslationsAlternative(@Path("animeId") animeId: Long,
                                   @Path("episodeId") episodeId: Long,
                                   @Query("type") type: String
    ): Single<List<TranslationResponse>>

    @GET("/api/anime/{animeId}/{episodeId}/video/{videoId}")
    fun getVideo(@Path("animeId") animeId: Long,
                 @Path("episodeId") episodeId: Int,
                 @Path("videoId") videoId: String,
                 @Query("language") language: String,
                 @Query("kind") type: String,
                 @Query("author") author: String,
                 @Query("hosting") hosting: String
    ): Single<VideoResponse>

    @POST("/api/anime/player")
    fun getVideo(@Body request : VideoRequest) : Single<VideoResponse>

    @Headers("Accept: text/html", "User-Agent: Mozilla/5.0 (Linux; Android 4.4; Nexus 5 Build/_BuildID_) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36")
    @GET
    fun getPlayerHtml(@Url playerUrl: String) : Single<ResponseBody>

    @Headers("Accept: text/plain")
    @GET
    fun getTextResponse(@Url playerUrl: String, @Header("Referer") referer: String? = null) : Single<ResponseBody>

    @GET
    fun getMailRuVideoMeta(@Url videoMetaUrl: String) : Single<Response<MailRuVideosResponse>>

    @GET
    fun getNuumStreamsMetadata(@Url metadataUrl: String) : Single<Response<NuumStreamsMetadataResponse>>

    @POST("https://www.cda.pl/")
    fun cdaApiRequest(@Body request : CdaApiRequest): Single<Response<CdaApiResponse>>

    @GET("https://kodik-api.com/search")
    fun getKodikSearch(@Query("token") token: String,
                       @Query("shikimori_id") shikimoriId: Long,
                       @Query("with_seasons") withSeasons: Boolean,
                       @Query("with_episodes") withEpisodes: Boolean
    ): Single<KodikSearchResponse>

    /**
     * The url is not fixed - kodik keeps the path base64'd in its player script so it can move it,
     * see KodikParser.
     */
    @FormUrlEncoded
    @POST
    fun getKodikLinks(@Url url: String, @FieldMap params: Map<String, String>): Single<Response<KodikLinksResponse>>

    /**
     * The full url is built by Anime365Parser - anime365 serves on four domains and the access
     * token goes in the query string.
     */
    @GET
    fun getAnime365Video(@Url url: String): Single<Anime365VideoResponse>

    @GET("/api/anime/alternative/translation/{id}")
    fun getVideoAlternative(
            @Path("id") translationId: Long,
            @Query("accessToken") accessToken : String? = null
    ): Single<VideoResponse>

    @GET("/api/anime/{animeId}/{episodeId}/topic")
    fun getTopic(@Path("animeId") animeId : Long,
                 @Path("episodeId") episodeId : Int
    ) : Single<EpisodeTopicIdResponse>
}
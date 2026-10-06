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
}

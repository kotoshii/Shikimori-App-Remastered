package com.gnoemes.shikimori.data.network

import com.gnoemes.shikimori.entity.app.data.GithubReleaseResponse
import io.reactivex.Single
import retrofit2.http.GET

interface GithubApi {

    @GET("repos/kotoshii/Shikimori-App-Remastered/releases/latest")
    fun getLatestRelease(): Single<GithubReleaseResponse>
}

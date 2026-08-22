package com.gnoemes.shikimori.entity.download

data class DownloadVideoData(
        val animeId : Long,
        val animeName : String,
        val episodeIndex : Int,
        val link : String?,
        //set when the hosting serves sound separately, downloaded alongside the video
        val audioLink : String? = null,
        val requestHeaders : Map<String, String>
)
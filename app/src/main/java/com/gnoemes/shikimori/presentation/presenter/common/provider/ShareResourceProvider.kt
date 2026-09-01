package com.gnoemes.shikimori.presentation.presenter.common.provider

interface ShareResourceProvider {

    /**
     * [details] are the parts describing the episode itself - author, kind, quality, hosting - and
     * are already filtered of blanks by the caller. An empty list simply leaves the line out.
     */
    fun getEpisodeShareFormattedMessage(title : String, episode : Int, url : String, details : List<String> = emptyList()) : String
}

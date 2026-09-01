package com.gnoemes.shikimori.entity.series.data.kodik

import com.google.gson.annotations.SerializedName

/**
 * Answer of the player's link call. `links` is keyed by quality - "360", "480", "720" and so on -
 * and is read as a map rather than as fixed fields so a quality kodik adds later is picked up on
 * its own.
 *
 * Every `src` is rotated and base64'd, see `KodikParserImpl.decodeLink`.
 */
data class KodikLinksResponse(
        @SerializedName("domain") val domain: String?,
        @SerializedName("default") val default: Int?,
        @SerializedName("links") val links: Map<String, List<Link>>?
) {

    data class Link(
            @SerializedName("src") val src: String?,
            @SerializedName("type") val type: String?
    )
}

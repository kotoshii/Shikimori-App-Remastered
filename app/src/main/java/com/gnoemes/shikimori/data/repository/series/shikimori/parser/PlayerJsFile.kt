package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.sortedByQuality

private val QUALITY_TAG = Regex("""^\s*\[(\d+)p\]""")

/**
 * The tracks in a PlayerJS `file` value, as AnimeJoy and AllVideo publish it: tagged links,
 * `[1080p]https://…,[360p]https://…`, or one untagged link. Best quality first.
 *
 * An untagged link gets [Track.UNKNOWN_QUALITY] - the whole url used to be taken as its quality, and
 * went into the quality menu and a download's file name (BUGS #1). A part that is not a full
 * `http(s)` address is skipped: AnimeJoy also points at PlayerJS playlist files by a site-relative
 * path (BUGS #59 N11), which nothing here can play.
 */
fun playerJsTracks(file: String?): List<Track> =
        file?.split(",")
                .orEmpty()
                .mapNotNull { part ->
                    val tag = QUALITY_TAG.find(part)
                    val url = (if (tag != null) part.substring(tag.range.last + 1) else part).trim()
                    val quality = tag?.groupValues?.get(1) ?: Track.UNKNOWN_QUALITY
                    if (url.startsWith("https://") || url.startsWith("http://")) Track(quality, url) else null
                }
                .sortedByQuality()

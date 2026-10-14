package com.gnoemes.shikimori.entity.series.presentation

import android.os.Parcelable
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.Video
import kotlinx.android.parcel.Parcelize

/**
 * A track in the download dialog. [author], [kind], [quality] and [hosting] describe it the same way
 * in a shared link and in a downloaded file's name; each is blank when not known.
 */
@Parcelize
data class SeriesDownloadItem(
        val track: Track,
        val video: Video,
        val author: String,
        val kind: String,
        val quality: String,
        val hosting: String
) : Parcelable
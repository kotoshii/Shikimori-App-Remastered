package com.gnoemes.shikimori.entity.download

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/** A single file saved as it is, with no playlist or remux step - a screenshot. */
@Parcelize
data class DownloadFileData(
        val url: String,
        //under the download folder, one entry per level, e.g. "screenshots" then the anime name
        val folders: List<String>,
        val name: String,
        //what the notification says is downloading
        val title: String
) : Parcelable

package com.gnoemes.shikimori.entity.series.presentation

import android.os.Parcelable
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import kotlinx.android.parcel.Parcelize

/**
 * What the watch screen showed, kept in its saved state so that it comes back the same after Android
 * kills the app in the background: the episode, the source and the type (P7-T5 Q65). [episodeId] is
 * null while the source's episode list has not been loaded; [type] is null until the type is known.
 */
@Parcelize
data class SeriesState(
        val episode: Int?,
        val episodeId: Long?,
        val isAlternative: Boolean,
        val type: TranslationType?
) : Parcelable

package com.gnoemes.shikimori.entity.chronology

import android.os.Parcelable
import com.gnoemes.shikimori.entity.common.domain.Type
import kotlinx.android.parcel.Parcelize

/**
 * «Хронология» of one title. [titleUrl] is the title's own page as the api gives it: some titles'
 * addresses carry a "z" before the id, so one built from the id is 404 for them (P7-T5, Test 61e).
 */
@Parcelize
data class ChronologyNavigationData(
        val id: Long,
        val type: Type,
        val titleUrl: String
) : Parcelable

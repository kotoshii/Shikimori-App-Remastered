package com.gnoemes.shikimori.entity.common.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class FilterItem(
        val action: String,
        val value: String?,
        val localizedText: String?,
        /**
         * Set for genres only, so the filter sheet can group them into demographic / genre / theme
         * sections. Everything else leaves it null. Deliberately not part of [equals] - two filters
         * are the same when their action and value are, which is what selection state compares.
         */
        val genreKind: GenreV2.Kind? = null
) : Parcelable {

    override fun toString(): String {
        return localizedText ?: ""
    }

    //the label plays no part: the studio and search items have none, and the old check compared them as
    //different objects only when this side had no label, so a == b did not mean b == a (BUGS #69 B14)
    override fun equals(other: Any?): Boolean = other is FilterItem && action == other.action && value == other.value

    override fun hashCode(): Int {
        var result = action.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }
}
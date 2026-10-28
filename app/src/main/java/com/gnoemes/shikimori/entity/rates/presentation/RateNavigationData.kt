package com.gnoemes.shikimori.entity.rates.presentation

import android.os.Parcelable
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import kotlinx.android.parcel.Parcelize

/**
 * A user's lists, opened from their profile. [nickname] and [avatar] are the profile's, null for one's
 * own: the list shows whose it is on its first frame, before its own read of the user comes back.
 */
@Parcelize
data class RateNavigationData(
        val userId : Long,
        val type : Type,
        val status : RateStatus,
        val nickname : String? = null,
        val avatar : String? = null
) : Parcelable
package com.gnoemes.shikimori.data.repository.user.converter

import com.gnoemes.shikimori.data.repository.common.ImageResponseConverter
import com.gnoemes.shikimori.entity.user.data.UserBriefResponse
import com.gnoemes.shikimori.entity.user.domain.UserBrief
import com.gnoemes.shikimori.utils.appendHostIfNeed
import javax.inject.Inject

class UserBriefResponseConverterImpl @Inject constructor(
        private val imageConverter: ImageResponseConverter
) : UserBriefResponseConverter {
    override fun apply(t: List<UserBriefResponse?>): List<UserBrief> =
            t.map { convertResponse(it)!! }

    override fun convertResponse(it: UserBriefResponse?): UserBrief? {
        if (it == null) {
            return null
        }

        return UserBrief(
                it.id,
                it.nickname,
                it.avatar?.appendHostIfNeed(),
                imageConverter.convertUserImage(it.image),
                it.dateLastOnline,
                it.name,
                it.sex,
                it.website,
                it.dateBirth,
                it.locale
        )
    }
}
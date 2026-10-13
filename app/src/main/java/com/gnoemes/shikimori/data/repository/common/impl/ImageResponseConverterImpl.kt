package com.gnoemes.shikimori.data.repository.common.impl

import com.gnoemes.shikimori.data.repository.common.ImageResponseConverter
import com.gnoemes.shikimori.entity.common.data.ImageResponse
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.user.data.UserImageResponse
import com.gnoemes.shikimori.entity.user.domain.UserImage
import com.gnoemes.shikimori.utils.appendHostIfNeed
import javax.inject.Inject

class ImageResponseConverterImpl @Inject constructor() : ImageResponseConverter {

    override fun convertResponse(response: ImageResponse): Image = Image(
            response.original?.appendHostIfNeed(),
            response.preview?.appendHostIfNeed(),
            response.x96?.appendHostIfNeed(),
            response.x48?.appendHostIfNeed()
    )

    override fun convertUserImage(response: UserImageResponse): UserImage = UserImage(
            response.x160?.appendHostIfNeed(),
            response.x148?.appendHostIfNeed(),
            response.x80?.appendHostIfNeed(),
            response.x64?.appendHostIfNeed(),
            response.x48?.appendHostIfNeed(),
            response.x32?.appendHostIfNeed(),
            response.x16?.appendHostIfNeed()
    )
}
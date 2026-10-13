package com.gnoemes.shikimori.data.repository.common

import com.gnoemes.shikimori.entity.common.data.ImageResponse
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.user.data.UserImageResponse
import com.gnoemes.shikimori.entity.user.domain.UserImage

interface ImageResponseConverter {

    fun convertResponse(response: ImageResponse): Image

    fun convertUserImage(response: UserImageResponse): UserImage
}
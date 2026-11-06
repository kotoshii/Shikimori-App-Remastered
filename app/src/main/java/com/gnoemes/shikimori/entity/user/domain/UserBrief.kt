package com.gnoemes.shikimori.entity.user.domain

import com.google.gson.annotations.SerializedName
import org.joda.time.DateTime

//saved on the device as JSON (UserSourceImpl): fixed names, so the release build's renaming cannot change
//them (BUGS #21)
data class UserBrief(
        @field:SerializedName("id") val id: Long,
        @field:SerializedName("nickname") val nickname: String,
        @field:SerializedName("avatar") val avatar: String?,
        @field:SerializedName("image") val image: UserImage,
        @field:SerializedName("dateLastOnline") val dateLastOnline: DateTime,
        @field:SerializedName("name") val name: String?,
        @field:SerializedName("sex") val sex: String?,
        @field:SerializedName("website") val website: String?,
        @field:SerializedName("dateBirth") val dateBirth: DateTime?,
        @field:SerializedName("locale") val locale: String?
)
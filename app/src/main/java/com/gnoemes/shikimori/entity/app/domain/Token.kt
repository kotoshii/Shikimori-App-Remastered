package com.gnoemes.shikimori.entity.app.domain

import com.google.gson.annotations.SerializedName

//saved on the device as JSON (TokenSourceImpl): fixed names, so the release build's renaming cannot change
//them and sign everyone out (BUGS #21)
data class Token(
        @field:SerializedName("authToken") val authToken: String,
        @field:SerializedName("refreshToken") val refreshToken: String
)
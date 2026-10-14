package com.gnoemes.shikimori.data.repository.common

import com.gnoemes.shikimori.entity.common.data.LinkedContentResponse
import com.gnoemes.shikimori.entity.common.domain.LinkedContent
import com.gnoemes.shikimori.entity.common.domain.Type
import com.google.gson.JsonElement

interface LinkedContentResponseConverter {

    fun convertResponse(it: LinkedContentResponse?): LinkedContent?

    /** [linked] as the api sends it, read as the class its [type] names; null for any other kind. */
    fun convertResponse(linked: JsonElement?, type: Type): LinkedContent?
}
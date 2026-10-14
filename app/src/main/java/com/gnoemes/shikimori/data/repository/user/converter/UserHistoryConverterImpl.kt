package com.gnoemes.shikimori.data.repository.user.converter

import com.gnoemes.shikimori.data.repository.common.LinkedContentResponseConverter
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.user.data.UserHistoryResponse
import com.gnoemes.shikimori.entity.user.domain.UserHistory
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import javax.inject.Inject

class UserHistoryConverterImpl @Inject constructor(
        private val linkedContentConverter: LinkedContentResponseConverter
) : UserHistoryConverter {

    override fun apply(t: List<UserHistoryResponse>): List<UserHistory> =
            t.map { convertResponse(it) }

    private fun convertResponse(it: UserHistoryResponse): UserHistory = UserHistory(
            it.id,
            it.dateCreated,
            it.description,
            linkedContentConverter.convertResponse(it.target, targetType(it.target))
    )

    //an entry does not say what its title is; the title's own address does: "/animes/…",
    //"/mangas/…" or "/ranobe/…"
    private fun targetType(target: JsonElement?): Type {
        val url = (target as? JsonObject)?.get("url")?.takeIf { it.isJsonPrimitive }?.asString ?: return Type.UNKNOWN
        return when {
            url.startsWith("/animes/") -> Type.ANIME
            url.startsWith("/mangas/") -> Type.MANGA
            url.startsWith("/ranobe/") -> Type.RANOBE
            else -> Type.UNKNOWN
        }
    }
}
package com.gnoemes.shikimori.entity.manga.domain

import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.LinkedContent
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.common.domain.Type
import org.joda.time.DateTime

data class Manga(
        val id: Long,
        val name: String,
        val nameRu: String?,
        val image: Image,
        val url: String,
        val type: MangaType,
        val score : Double?,
        val status: Status,
        val volumes: Int,
        val chapters: Int,
        val dateAired: DateTime?,
        val dateReleased: DateTime?,
        val isRanobe: Boolean = type == MangaType.NOVEL || type == MangaType.LIGHT_NOVEL
        //a novel is a ranobe wherever it is listed - saying MANGA here opened ranobe in manga mode
        //from search, chronology, similar and related alike, which then searched manga on a genre tap
) : LinkedContent(id, if (isRanobe) Type.RANOBE else Type.MANGA, image.original, name)
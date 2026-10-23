package com.gnoemes.shikimori.utils

import androidx.annotation.StringRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.anime.domain.AnimeType
import com.gnoemes.shikimori.entity.manga.domain.MangaType

/**
 * A title's kind in the site's words, as on its cards, title pages and «Тип» filter: «TV Сериал»,
 * «Спецвыпуск». The site's table view of a list has a short set too («Сериал», «Спешл»); the app has
 * room for the full words everywhere. The cards, the title pages, the forum's title block and the
 * catalog's «Тип» filter all take it from here. Null for a title with no kind.
 */
@get:StringRes
val AnimeType.labelRes: Int?
    get() = when (this) {
        //the last three are the «Тип» filter's TV by episode count; a title's own kind is plain tv
        AnimeType.TV, AnimeType.TV_13, AnimeType.TV_24, AnimeType.TV_48 -> R.string.type_tv_translatable
        AnimeType.MOVIE -> R.string.type_movie_translatable
        AnimeType.OVA -> R.string.type_ova
        AnimeType.ONA -> R.string.type_ona
        AnimeType.SPECIAL -> R.string.type_special_full_translatable
        AnimeType.TV_SPECIAL -> R.string.type_tv_special_full_translatable
        AnimeType.MUSIC -> R.string.type_music_translatable
        AnimeType.PV -> R.string.type_pv_translatable
        AnimeType.CM -> R.string.type_cm_translatable
        AnimeType.NONE -> null
    }

/** «Манга», «Ранобэ», «Новелла»…; null for an unknown kind. */
@get:StringRes
val MangaType.labelRes: Int?
    get() = when (this) {
        MangaType.MANGA -> R.string.type_manga_translatable
        MangaType.MANHWA -> R.string.type_manhwa_translatable
        MangaType.MANHUA -> R.string.type_manhua_translatable
        MangaType.NOVEL -> R.string.type_novella_translatable
        MangaType.LIGHT_NOVEL -> R.string.type_novel_translatable
        MangaType.ONE_SHOT -> R.string.type_one_shot_translatable
        MangaType.DOUJIN -> R.string.type_doujin_translatable
        MangaType.UNKNOWN -> null
    }

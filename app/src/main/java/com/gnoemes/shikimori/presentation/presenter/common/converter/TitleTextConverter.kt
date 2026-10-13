package com.gnoemes.shikimori.presentation.presenter.common.converter

import android.content.Context
import android.text.SpannableStringBuilder
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.anime.domain.AnimeType
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.manga.domain.MangaType
import com.gnoemes.shikimori.utils.color
import com.gnoemes.shikimori.utils.colorSpan
import com.gnoemes.shikimori.utils.faintTextColor
import javax.inject.Inject

/**
 * A title's type label and the line under its name on the cards of «Похожее», «Хронология» and the
 * list, written once: «Онгоинг  •  TV  •  5/12 эп.», «2019  •  TV  •  24 эп.».
 */
class TitleTextConverter @Inject constructor(
        private val context: Context
) {

    /** «TV», «OVA», «Фильм»…; null for a kind the app has no label for. */
    fun typeLabel(type: AnimeType): String? = when (type) {
        AnimeType.TV -> context.getString(R.string.type_tv_short_translatable)
        AnimeType.OVA -> context.getString(R.string.type_ova)
        AnimeType.ONA -> context.getString(R.string.type_ona)
        AnimeType.MUSIC -> context.getString(R.string.type_music_translatable)
        AnimeType.MOVIE -> context.getString(R.string.type_movie_translatable)
        AnimeType.SPECIAL -> context.getString(R.string.type_special_translatable)
        else -> null
    }

    /** «Манга», «Ранобэ», «Ваншот»…; null for an unknown kind. */
    fun typeLabel(type: MangaType): String? = when (type) {
        MangaType.MANGA -> context.getString(R.string.type_manga_translatable)
        MangaType.NOVEL -> context.getString(R.string.type_novel_translatable)
        MangaType.LIGHT_NOVEL -> context.getString(R.string.type_novel_translatable)
        MangaType.ONE_SHOT -> context.getString(R.string.type_one_shot_translatable)
        MangaType.DOUJIN -> context.getString(R.string.type_doujin_translatable)
        MangaType.MANHUA -> context.getString(R.string.type_manhua_translatable)
        MangaType.MANHWA -> context.getString(R.string.type_manhwa_translatable)
        MangaType.UNKNOWN -> null
    }

    /** Status, start year (as the site shows it), type, episodes. */
    fun describe(anime: Anime): CharSequence {
        val line = Line()
        line.addStatus(anime.status)
        if (anime.status != Status.ONGOING) line.add(anime.dateAired?.year?.toString())
        line.add(typeLabel(anime.type))

        val episodeShort = context.getString(R.string.episode_short)
        when {
            anime.type == AnimeType.MOVIE -> Unit
            anime.status == Status.ANONS -> if (anime.episodes != 0) line.add("${anime.episodes}")
            anime.status == Status.ONGOING -> line.add("${anime.episodesAired}/${anime.episodes.dashIfZero()} $episodeShort")
            else -> line.add("${anime.episodes} $episodeShort")
        }
        return line.text
    }

    /** Status, start year, type, chapters. */
    fun describe(manga: Manga): CharSequence {
        val line = Line()
        line.addStatus(manga.status)
        if (manga.status != Status.ONGOING) line.add(manga.dateAired?.year?.toString())
        line.add(typeLabel(manga.type))

        when (manga.status) {
            Status.ANONS -> if (manga.chapters != 0) line.add("${manga.chapters}")
            Status.ONGOING -> if (manga.chapters != 0) line.add(chapters(manga.chapters))
            else -> line.add(chapters(manga.chapters))
        }
        return line.text
    }

    private fun chapters(count: Int): String =
            context.resources.getQuantityString(R.plurals.chapters, count, count)

    private fun Int.dashIfZero(): String = if (this == 0) "-" else toString()

    /** Parts joined by a faint «•»; an empty part is skipped. */
    private inner class Line {
        val text = SpannableStringBuilder()
        private val divider = "  •  ".colorSpan(context.faintTextColor)

        fun add(part: CharSequence?) {
            if (part.isNullOrEmpty()) return
            if (text.isNotEmpty()) text.append(divider)
            text.append(part)
        }

        fun addStatus(status: Status) = when (status) {
            Status.ANONS -> add(context.getString(R.string.status_anons).colorSpan(context.color(R.color.status_anons)))
            Status.ONGOING -> add(context.getString(R.string.status_ongoing).colorSpan(context.color(R.color.status_ongoing)))
            else -> Unit
        }
    }
}

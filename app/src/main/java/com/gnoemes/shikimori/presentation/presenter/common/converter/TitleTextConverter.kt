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
import com.gnoemes.shikimori.utils.colorRes
import com.gnoemes.shikimori.utils.colorSpan
import com.gnoemes.shikimori.utils.faintTextColor
import com.gnoemes.shikimori.utils.labelRes
import javax.inject.Inject

/**
 * A title's type label and the line under its name on the cards of «Похожее», «Хронология» and the
 * list, written once: «Онгоинг  •  TV Сериал  •  5/12 эп.», «2019  •  TV Сериал  •  24 эп.».
 */
class TitleTextConverter @Inject constructor(
        private val context: Context
) {

    /** «TV Сериал», «OVA», «Спецвыпуск»…: the site's words, as on its cards; null for no kind. */
    fun typeLabel(type: AnimeType): String? = type.labelRes?.let { context.getString(it) }

    /** «Манга», «Ранобэ», «Ваншот»…; null for an unknown kind. */
    fun typeLabel(type: MangaType): String? = type.labelRes?.let { context.getString(it) }

    /** Status, start year (as the site shows it), type, episodes. */
    fun describe(anime: Anime): CharSequence {
        val line = Line()
        line.addStatus(anime.status, true)
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
        line.addStatus(manga.status, false)
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

        fun addStatus(status: Status, isAnime: Boolean) {
            val color = status.colorRes ?: return
            add(context.getString(status.labelRes(isAnime)).colorSpan(context.color(color)))
        }
    }
}

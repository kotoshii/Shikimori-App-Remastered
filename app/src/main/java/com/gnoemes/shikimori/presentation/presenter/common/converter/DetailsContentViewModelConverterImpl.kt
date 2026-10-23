package com.gnoemes.shikimori.presentation.presenter.common.converter

import android.content.Context
import android.text.SpannableStringBuilder
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.anime.domain.*
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.Related
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.ContentItem
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentItem
import com.gnoemes.shikimori.entity.common.presentation.FrameItem
import com.gnoemes.shikimori.entity.common.presentation.PlaceholderItem
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.manga.domain.MangaType
import com.gnoemes.shikimori.entity.roles.domain.Character
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.entity.roles.domain.Work
import com.gnoemes.shikimori.utils.colorSpan
import com.gnoemes.shikimori.utils.faintTextColor
import javax.inject.Inject
import org.joda.time.DateTime

class DetailsContentViewModelConverterImpl @Inject constructor(
        private val context: Context,
        private val settings: SettingsSource,
        private val titleText: TitleTextConverter
) : DetailsContentViewModelConverter {

    private val dividerColor: Int
        get() = context.faintTextColor

    override fun apply(t: List<Any>): DetailsContentItem {
        val items = convertItems(t)
        return DetailsContentItem(items)
    }

    private fun convertItems(t: List<Any>): List<Any> {
        return t.mapNotNull {
            when (it) {
                is Anime -> convertAnime(it)
                is Manga -> convertManga(it)
                is Character -> convertCharacter(it)
                is Person -> convertPerson(it)
                is Related -> convertRelated(it)
                is AnimeVideo -> convertVideo(it)
                is Work -> convertWork(it)
                is Screenshot -> convertScreenshot(it)
                is PlaceholderItem -> it
                else -> null
            }
        }
    }

    private fun convertScreenshot(it: Screenshot): FrameItem {
        val image = Image(it.original, it.preview, null, null)
        return FrameItem(image, null, null, it)
    }

    private fun convertWork(it: Work): ContentItem {
        val isAnime = it.type == Type.ANIME

        val name = if (isAnime) it.anime?.let { anime -> settings.titleName(anime.name, anime.nameRu) }
        else it.manga?.let { manga -> settings.titleName(manga.name, manga.nameRu) }

        val descriptionText = it.role

        val image = if (isAnime) it.anime?.image!! else it.manga?.image!!

        return ContentItem(
                name!!,
                image,
                descriptionText,
                it
        )
    }

    private fun convertVideo(it: AnimeVideo): FrameItem {
        return FrameItem(
                Image(it.imageUrl, it.imageUrl, it.imageUrl, it.imageUrl),
                it.name ?: context.getString(getTitleFromType(it.type)),
                it.hosting?.toUpperCase(),
                it
        )
    }

    private fun convertRelated(it: Related): ContentItem {
        val isAnime = it.type == Type.ANIME

        val relations = mutableListOf(
                Pair("Adaptation", context.getString(R.string.chronology_adaptation)),
                Pair("Prequel", context.getString(R.string.chronology_prequel)),
                Pair("Side Story", context.getString(R.string.chronology_side_story)),
                Pair("Sequel", context.getString(R.string.chronology_sequel)),
                Pair("Sequel", context.getString(R.string.chronology_sequel)),
                Pair("Alternative Setting", context.getString(R.string.chronology_alternative_setting)),
                Pair("Alternative Version", context.getString(R.string.chronology_alternative_version)),
                Pair("Other", context.getString(R.string.chronology_other)),
                Pair("Summary", context.getString(R.string.chronology_summary)),
                Pair("Full Story", context.getString(R.string.chronology_parent_story)),
                Pair("Parent Story", context.getString(R.string.chronology_parent_story)),
                Pair("Character", context.getString(R.string.common_character))
        )

        val name = relations.find { pair -> pair.first.contains(it.relation, true) }?.second
                ?: it.relationRu
                ?: it.relation
        val description =
                if (isAnime) typeAndYear(getLocalizedType(it.anime!!.type), it.anime.dateAired)
                else typeAndYear(getLocalizedType(it.manga!!.type), it.manga.dateAired)

        val image = if (isAnime) it.anime?.image!! else it.manga?.image!!

        return ContentItem(
                name,
                image,
                description,
                it

        )
    }

    private fun convertPerson(it: Person): ContentItem {
        val name = settings.titleName(it.name, it.nameRu)
        return ContentItem(
                name,
                it.image,
                null,
                it
        )
    }

    private fun convertCharacter(it: Character): ContentItem {
        val name = settings.titleName(it.name, it.nameRu)
        return ContentItem(
                name,
                it.image,
                null,
                it
        )
    }

    private fun convertManga(it: Manga): ContentItem {
        val name = settings.titleName(it.name, it.nameRu)
        return ContentItem(
                name,
                it.image,
                null,
                it
        )
    }

    private fun convertAnime(it: Anime): ContentItem {
        val name = settings.titleName(it.name, it.nameRu)
        return ContentItem(
                name,
                it.image,
                typeAndYear(getLocalizedType(it.type), it.dateAired),
                it
        )
    }

    /** «TV Сериал  •  2019»: the type and the start year, as the site's cards show them; no start date, no year. */
    private fun typeAndYear(type: String, dateAired: DateTime?): CharSequence {
        val text = SpannableStringBuilder(type)
        if (dateAired != null) {
            text.append("  •  ".colorSpan(dividerColor))
            text.append("${dateAired.year}".colorSpan(dividerColor))
        }
        return text
    }

    private fun getLocalizedType(type: AnimeType): String =
            titleText.typeLabel(type) ?: context.getString(R.string.type_tv_short_translatable)

    private fun getLocalizedType(type: MangaType): String =
            titleText.typeLabel(type) ?: context.getString(R.string.type_manga_translatable)

    private fun getTitleFromType(videoType: AnimeVideoType): Int {
        return when (videoType) {
            AnimeVideoType.PROMO -> R.string.promo
            AnimeVideoType.ENDING -> R.string.ending
            AnimeVideoType.OPENING -> R.string.opening
            AnimeVideoType.OTHER -> R.string.other
        }
    }
}
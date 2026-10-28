package com.gnoemes.shikimori.presentation.presenter.manga.converter

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.*
import com.gnoemes.shikimori.entity.manga.domain.MangaDetails
import com.gnoemes.shikimori.entity.manga.domain.MangaType
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.presentation.presenter.common.converter.BBCodesTextProcessor
import com.gnoemes.shikimori.presentation.presenter.common.converter.TitleTextConverter
import com.gnoemes.shikimori.presentation.presenter.details.BaseTitleDetailsViewModelConverter
import com.gnoemes.shikimori.utils.date.DateTimeConverter
import javax.inject.Inject

class MangaDetailsViewModelConverterImpl @Inject constructor(
        context: Context,
        settings: SettingsSource,
        dateTimeConverter: DateTimeConverter,
        textProcessor: BBCodesTextProcessor,
        private val titleText: TitleTextConverter
) : BaseTitleDetailsViewModelConverter<MangaDetails>(context, settings, dateTimeConverter, textProcessor), MangaDetailsViewModelConverter {

    companion object {
        private val CREATOR_ROLES = mapOf(
                "Art" to R.string.person_art,
                "Story" to R.string.person_story,
                "Story & Art" to R.string.person_story_and_art
        )
    }


    override val statusLabels = listOf(
            "Reading|Читаю" to R.string.rate_reading,
            "Planned to Read|Запланировано" to R.string.rate_planned,
            "Completed|Прочитано" to R.string.rate_readed,
            "On Hold|Отложено" to R.string.rate_on_hold,
            "Dropped|Брошено" to R.string.rate_dropped
    )

    override fun convertInfo(it: MangaDetails, creators: List<Pair<Person, List<String>>>): DetailsInfoItem {
        val tags = genreTags(it.genres)

        val info = mutableListOf<Any>()

        //ongoing or anons indicator
        statusItem(it.status, false)?.let { item -> info.add(item) }

        //release date
        if (it.status == Status.ANONS && it.dateAired != null) {
            info.add(anonsDateItem(it.dateAired))
        } else if (it.status == Status.ONGOING && it.dateAired != null) {
            val description = dateTimeConverter.convertAnimeSeasonToString(it.dateAired)
            val category = context.getString(R.string.details_release_date)
            info.add(InfoItem(description, category))
        } else if (it.dateReleased != null && it.dateAired != null) {
            val description = "${it.dateAired.year} - ${it.dateReleased.year}"
            val category = context.getString(R.string.details_released_manga)
            info.add(InfoItem(description, category))
        }

        //type
        run {
            val description = getLocalizedType(it.type)
            val category = context.getString(R.string.common_type)
            info.add(InfoItem(description, category))
        }

        //volumes
        if (it.volumes != 0) {
            val description = it.volumes.toString()
            val category = context.getString(R.string.common_volumes)
            info.add(InfoItem(description, category))
        }

        //chapters
        if (it.chapters != 0) {
            val description = it.chapters.toString()
            val category = context.getString(R.string.common_chapters)
            info.add(InfoItem(description, category))
        }

        info.addAll(creatorItems(creators, CREATOR_ROLES, "Story"))

        return DetailsInfoItem(secondName(it), tags, info)
    }

    private fun getLocalizedType(type: MangaType): String = titleText.typeLabel(type) ?: ""
}

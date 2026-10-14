package com.gnoemes.shikimori.presentation.presenter.anime.converter

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.entity.anime.domain.AnimeDetails
import com.gnoemes.shikimori.entity.anime.domain.AnimeType
import com.gnoemes.shikimori.entity.common.domain.AgeRating
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.*
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.presentation.presenter.common.converter.BBCodesTextProcessor
import com.gnoemes.shikimori.presentation.presenter.common.converter.TitleTextConverter
import com.gnoemes.shikimori.presentation.presenter.details.BaseTitleDetailsViewModelConverter
import com.gnoemes.shikimori.utils.date.DateTimeConverter
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.Interval
import javax.inject.Inject

class AnimeDetailsViewModelConverterImpl @Inject constructor(
        context: Context,
        settings: SettingsSource,
        dateTimeConverter: DateTimeConverter,
        textProcessor: BBCodesTextProcessor,
        private val titleText: TitleTextConverter
) : BaseTitleDetailsViewModelConverter<AnimeDetails>(context, settings, dateTimeConverter, textProcessor), AnimeDetailsViewModelConverter {

    companion object {
        private val CREATOR_ROLES = mapOf(
                "Director" to R.string.person_director,
                "Original Creator" to R.string.person_original_creator
        )
    }


    override val statusLabels = listOf(
            "Watching|Смотрю" to R.string.rate_watching,
            "Planned to Watch|Запланировано" to R.string.rate_planned,
            "Completed|Просмотрено" to R.string.rate_completed,
            "On Hold|Отложено" to R.string.rate_on_hold,
            "Dropped|Брошено" to R.string.rate_dropped
    )

    override fun convertInfo(it: AnimeDetails, creators: List<Pair<Person, List<String>>>): DetailsInfoItem {
        val tags = mutableListOf<DetailsTagItem>()

        it.studios.firstOrNull()?.let {
            tags.add(DetailsTagItem(it.id, DetailsTagItem.TagType.STUDIO, it.name, it))
        }
        tags.addAll(genreTags(it.genres))

        val info = mutableListOf<Any>()

        //ongoing or anons indicator
        statusItem(it.status)?.let { item -> info.add(item) }

        //release date (or next episode date for ongoings)
        if (it.status == Status.ANONS && it.dateAired != null) {
            info.add(anonsDateItem(it.dateAired))
        } else if (it.status == Status.ONGOING && it.nextEpisodeDate != null) {
            val now = DateTime.now()
            if (now.isBefore(it.nextEpisodeDate)) {
                val duration = Interval(now, it.nextEpisodeDate).toDuration()
                val description =
                        when {
                            duration.standardHours == 0L -> duration.toMinutes()
                            duration.standardDays == 0L -> duration.toHoursAndMinutes()
                            else -> duration.toDays()
                        }
                val category = String.format(context.getString(R.string.details_release_episode_format), it.episodesAired + 1)
                info.add(InfoItem(description, category))
            }
        } else if (it.dateAired != null && it.dateReleased != null) {

            val description = if (it.dateAired.year == it.dateReleased.year) {
                "${it.dateAired.toString("dd ${dateTimeConverter.convertShortMonth(it.dateAired)}")} - ${it.dateReleased.toTitleDate()}"
            } else {
                "${it.dateAired.toTitleDate()} - ${it.dateReleased.toTitleDate()}"
            }
            val category = context.getString(R.string.details_release_date)
            info.add(InfoItem(description, category))
        } else {
            if (it.dateAired != null) {
                val description = dateTimeConverter.convertAnimeSeasonToString(it.dateAired)
                val category = context.getString(R.string.details_release_date)
                info.add(InfoItem(description, category))
            }

            if (it.dateReleased != null) {
                val description = dateTimeConverter.convertAnimeSeasonToString(it.dateReleased)
                val category = context.getString(R.string.details_released)
                info.add(InfoItem(description, category))
            }
        }

        //type
        run {
            val description = getLocalizedType(it.type)
            val category = context.getString(R.string.common_type)
            info.add(InfoItem(description, category))
        }

        //episodes
        if (it.episodes > 1 || (it.status == Status.ONGOING && it.episodes == 0)) {
            val category = context.getString(R.string.common_episodes)
            if (it.status == Status.ONGOING) {
                val description = "${it.episodesAired}/" + if (it.episodes == 0) "-" else it.episodes.toString()
                info.add(InfoItem(description, category))
            } else {
                val description = it.episodes.toString()
                info.add(InfoItem(description, category))
            }
        }

        //duration
        if (it.duration != 0) {
            val description = Duration.standardMinutes(it.duration.toLong()).let {
                when {
                    it.standardHours == 0L -> it.toMinutes()
                    it.standardMinutes % 60 == 0L -> it.toHours()
                    else -> it.toHoursAndMinutes()
                }
            }
            val category = context.getString(R.string.details_episode_duration)
            info.add(InfoItem(description, category))
        }

        //age rating
        if (it.ageRating != AgeRating.NONE) {
            val description = convertRating(it.ageRating)
            if (description != null) {
                val category = context.getString(R.string.common_age_rating)
                info.add(InfoItem(description, category))
            }
        }

        //creators
        info.addAll(creatorItems(creators, CREATOR_ROLES, "Director"))

        return DetailsInfoItem(secondName(it), tags, info)
    }

    private fun getLocalizedType(type: AnimeType): String =
            titleText.typeLabel(type) ?: context.getString(R.string.type_tv_short_translatable)

    private fun convertRating(ageRating: AgeRating): String? {
        val names = context.resources.getStringArray(R.array.age_ratings_values)
        val values = AgeRating.values().filter { it.rating != AgeRating.NONE.rating }
        return names.zip(values)
                .find { it.second == ageRating }
                ?.first
    }

    private fun Duration.toMinutes(): String = "${standardMinutes % 60} ${context.getString(R.string.minute_short)}"
    private fun Duration.toHours(): String = "$standardHours ${context.getString(R.string.hour_short)}"
    private fun Duration.toHoursAndMinutes(): String = "$standardHours ${context.getString(R.string.hour_short)} ${toMinutes()}"
    private fun Duration.toDays(): String = context.resources.getQuantityString(R.plurals.days, standardDays.toInt(), standardDays)
}

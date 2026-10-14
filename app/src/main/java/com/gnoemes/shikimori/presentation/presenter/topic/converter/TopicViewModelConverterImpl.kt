package com.gnoemes.shikimori.presentation.presenter.topic.converter

import android.content.Context
import androidx.annotation.StringRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.common.domain.LinkedContent
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.roles.domain.Character
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.entity.topic.domain.Topic
import com.gnoemes.shikimori.entity.topic.domain.TopicEvent
import com.gnoemes.shikimori.entity.topic.domain.TopicType
import com.gnoemes.shikimori.entity.topic.presentation.TopicContentViewModel
import com.gnoemes.shikimori.entity.topic.presentation.TopicLinkedViewModel
import com.gnoemes.shikimori.entity.topic.presentation.TopicUserViewModel
import com.gnoemes.shikimori.entity.topic.presentation.TopicViewModel
import com.gnoemes.shikimori.presentation.presenter.common.converter.BBCodesTextProcessor
import com.gnoemes.shikimori.presentation.presenter.common.converter.TitleTextConverter
import com.gnoemes.shikimori.presentation.presenter.common.converter.labelRes
import com.gnoemes.shikimori.utils.color
import com.gnoemes.shikimori.utils.date.DateTimeConverter
import com.gnoemes.shikimori.utils.tag
import com.gnoemes.shikimori.utils.toBold
import com.gnoemes.shikimori.utils.unknownIfZero
import javax.inject.Inject

class TopicViewModelConverterImpl @Inject constructor(
        private val context: Context,
        private val dateTimeConverter: DateTimeConverter,
        private val textProcessor: BBCodesTextProcessor,
        private val settings: SettingsSource,
        private val titleText: TitleTextConverter
) : TopicViewModelConverter {

    override fun apply(t: List<Topic>): List<TopicViewModel> =
            t.map { convertTopic(it) }

    override fun convertTopic(it: Topic): TopicViewModel {
        val createdDate = dateTimeConverter.convertDateAgoToString(it.dateCreated)

        //a new episode or a release first, else the kind of topic
        val typeTag = it.type.tag
        val tag = getTag(it.event, it.episode) ?: typeTag?.let { tag -> context.getString(tag.text) }
        val tagColor = (getTagColor(it.event) ?: typeTag?.color)?.let { res -> context.color(res) }

        val userViewModel = TopicUserViewModel(it.user, createdDate, tag, tagColor)
        val contentViewModel = TopicContentViewModel(it.title, it.description?.let { textProcessor.process(it) })

        return TopicViewModel(
                it.id,
                userViewModel,
                contentViewModel,
                it.commentsCount,
                it.type,
                convertLinked(it.linked),
                it.isViewed,
                it.event,
                it.episode
        )
    }

    /** The title block, for the forum list and the topic page alike. */
    private fun convertLinked(linked: LinkedContent?): TopicLinkedViewModel? = when (linked) {
        null -> null
        is Anime -> TopicLinkedViewModel(linked.linkedType, linked.linkedId, linked.imageUrl,
                settings.titleName(linked.name, linked.nameRu),
                line(R.string.details_type, String.format(context.getString(R.string.type_pattern_without_duration),
                        titleText.typeLabel(linked.type) ?: linked.type.type.toUpperCase(), linked.episodes.unknownIfZero())),
                line(R.string.details_season, dateTimeConverter.convertAnimeSeasonToString(linked.dateAired)),
                line(R.string.details_status, context.getString(linked.status.labelRes)))
        is Manga -> TopicLinkedViewModel(linked.linkedType, linked.linkedId, linked.imageUrl,
                settings.titleName(linked.name, linked.nameRu),
                line(R.string.details_type, String.format(context.getString(R.string.type_pattern_manga),
                        titleText.typeLabel(linked.type).orEmpty(), linked.volumes.unknownIfZero(), linked.chapters.unknownIfZero())),
                line(R.string.details_season, dateTimeConverter.convertAnimeSeasonToString(linked.dateAired)),
                line(R.string.details_status, context.getString(linked.status.labelRes)))
        is Character -> TopicLinkedViewModel(linked.linkedType, linked.linkedId, linked.imageUrl, settings.titleName(linked.name, linked.nameRu))
        is Person -> TopicLinkedViewModel(linked.linkedType, linked.linkedId, linked.imageUrl, settings.titleName(linked.name, linked.nameRu))
        else -> TopicLinkedViewModel(linked.linkedType, linked.linkedId, linked.imageUrl, linked.linkedName)
    }

    //«Тип: TV (12 эп.)»: the label in bold
    private fun line(@StringRes label: Int, value: String): CharSequence =
            context.getString(label).toBold().append(" ").append(value)

    private fun getTagColor(event: TopicEvent): Int? {
        return when (event) {
            TopicEvent.RELEASED -> R.color.topic_release
            TopicEvent.EPISODE -> R.color.topic_new_episode
            else -> null
        }
    }

    private fun getTag(event: TopicEvent, episode: String?): String? {
        return when {
            event == TopicEvent.EPISODE || !episode.isNullOrBlank() -> "$episode ${context.getString(R.string.topic_tag_episode)}"
            event == TopicEvent.RELEASED -> context.getString(R.string.topic_tag_release)
            else -> null
        }
    }
}
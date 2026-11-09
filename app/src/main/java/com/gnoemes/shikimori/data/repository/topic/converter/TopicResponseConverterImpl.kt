package com.gnoemes.shikimori.data.repository.topic.converter

import com.gnoemes.shikimori.data.repository.common.LinkedContentResponseConverter
import com.gnoemes.shikimori.data.repository.user.converter.UserBriefResponseConverter
import com.gnoemes.shikimori.entity.topic.data.TopicResponse
import com.gnoemes.shikimori.entity.topic.domain.Topic
import com.gnoemes.shikimori.entity.topic.domain.TopicType
import com.gnoemes.shikimori.utils.appendHostIfNeed
import org.jsoup.parser.Parser
import javax.inject.Inject

class TopicResponseConverterImpl @Inject constructor(
        private val userConverter: UserBriefResponseConverter,
        private val linkedConverter: LinkedContentResponseConverter
) : TopicResponseConverter {

    override fun apply(t: List<TopicResponse>): List<Topic> = t.mapNotNull { convertResponse(it) }

    override fun convertResponse(it: TopicResponse?): Topic? {
        if (it == null) {
            return null
        }

        return Topic(
                it.id,
                //plain text: the site builds critique and review titles from its templates for HTML pages, with
                //`&laquo;`/`&raquo;` in them (`config/locales/view_objects.ru.yml`), while users' own titles come as
                //typed (live 2026-11-09). Decoded as in an attribute, so `&not` in «R&D &notes» stays as written
                Parser.unescapeEntities(it.title.orEmpty(), true),
                it.description,
                it.descriptionHtml,
                it.footer,
                it.dateCreated,
                it.commentsCount,
                it.forum.name,
                //`/topics/<id>` is no page (404, live 2026-11-09); `/forum/<forum>/<id>` redirects to the topic
                "${it.forum.url.trimEnd('/')}/${it.id}".appendHostIfNeed(),
                userConverter.convertResponse(it.user)!!,
                convertType(it.type, it.description, it.descriptionHtml),
                it.linkedType,
                linkedConverter.convertResponse(it.linked, it.linkedType),
                it.isViewed,
                it.event,
                it.episode
        )
    }

    private fun convertType(type: String?, description: String?, html: String?): TopicType {
        if (type.isNullOrEmpty()) {
            return TopicType.DEFAULT
        }

        return TopicType.values()
                .filter { it.isEqualType(type) }
                .map {
                    when {
                        (it == TopicType.NEWS || it == TopicType.NEWS_LINK_ONLY) && description.isNullOrEmpty() && html.isNullOrEmpty() -> TopicType.NEWS_LINK_ONLY
                        else -> it
                    }
                }.firstOrNull() ?: TopicType.DEFAULT

    }
}
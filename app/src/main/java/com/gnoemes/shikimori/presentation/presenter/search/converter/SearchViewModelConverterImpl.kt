package com.gnoemes.shikimori.presentation.presenter.search.converter

import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.roles.domain.Character
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.entity.search.domain.CatalogSettings
import com.gnoemes.shikimori.entity.search.presentation.SearchItem
import com.gnoemes.shikimori.presentation.presenter.common.converter.TitleTextConverter
import javax.inject.Inject

class SearchViewModelConverterImpl @Inject constructor(
        private val settings: SettingsSource,
        //the type in the site's words and the line under the name, as the other cards write them
        private val titleText: TitleTextConverter
) : SearchViewModelConverter {

    override fun convert(items: List<Any>, settings: CatalogSettings, statuses: Map<Long, RateStatus>): List<SearchItem> =
            items.mapNotNull { convertItem(it, settings, statuses) }

    private fun convertItem(item: Any, look: CatalogSettings, statuses: Map<Long, RateStatus>): SearchItem? =
            when (item) {
                is Anime -> SearchItem(
                        item.id,
                        Type.ANIME,
                        item.image,
                        name(item.name, item.nameRu, look),
                        titleText.typeLabel(item.type)?.takeIf { look.showType },
                        if (look.showsLine) titleText.describe(item, lineParts(look)) else null,
                        score(item.score, look),
                        statuses[item.id]?.takeIf { look.showStatus }
                )
                is Manga -> SearchItem(
                        item.id,
                        if (item.isRanobe) Type.RANOBE else Type.MANGA,
                        item.image,
                        name(item.name, item.nameRu, look),
                        titleText.typeLabel(item.type)?.takeIf { look.showType },
                        if (look.showsLine) titleText.describe(item, lineParts(look)) else null,
                        score(item.score, look),
                        statuses[item.id]?.takeIf { look.showStatus }
                )
                //the picture and the name only
                is Character -> SearchItem(item.id, Type.CHARACTER, item.image, name(item.name, item.nameRu, look), null, null, null, null)
                is Person -> SearchItem(item.id, Type.PERSON, item.image, name(item.name, item.nameRu, look), null, null, null, null)
                else -> null
            }

    private fun name(name: String, nameRu: String?, look: CatalogSettings): String? =
            if (look.showName) settings.titleName(name, nameRu) else null

    private val CatalogSettings.showsLine: Boolean
        get() = showYear || showCount

    //the type is on the poster, so the line leaves it out: «2019  •  24 эп.»
    private fun lineParts(look: CatalogSettings) =
            TitleTextConverter.Parts(statusAndYear = look.showYear, type = false, count = look.showCount)

    //as the title page shows it; none for an announcement or a title with too few votes
    private fun score(score: Double?, look: CatalogSettings): String? =
            score?.takeIf { look.showScore && it > 0 }?.toString()
}

package com.gnoemes.shikimori.presentation.presenter.search.converter

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.roles.domain.Character
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.entity.search.presentation.SearchItem
import com.gnoemes.shikimori.presentation.presenter.common.converter.TitleTextConverter
import com.gnoemes.shikimori.utils.unknownIfZero
import javax.inject.Inject

class SearchViewModelConverterImpl @Inject constructor(
        private val context: Context,
        private val settings: SettingsSource,
        //the type in the site's words, as its catalog cards show it: the api's raw kind (LIGHT_NOVEL) showed here
        private val titleText: TitleTextConverter
) : SearchViewModelConverter {
    override fun apply(t: List<Any>): List<SearchItem> =
            t.mapNotNull { convertItem(it) }

    private fun convertItem(item: Any): SearchItem? =
            when (item) {
                is Anime -> convertAnime(item)
                is Manga -> convertManga(item)
                is Character -> convertCharacter(item)
                is Person -> convertPerson(item)
                else -> null
            }

    private fun convertPerson(it: Person): SearchItem {
        val name = settings.titleName(it.name, it.nameRu)
        return SearchItem(
                it.id,
                Type.PERSON,
                name,
                it.image,
                null
        )
    }

    private fun convertCharacter(it: Character): SearchItem {
        val name = settings.titleName(it.name, it.nameRu)
        return SearchItem(
                it.id,
                Type.CHARACTER,
                name,
                it.image,
                null
        )
    }

    private fun convertManga(it: Manga): SearchItem {
        val name = settings.titleName(it.name, it.nameRu)
        val nameText = name.plus(String.format(context.getString(R.string.volumes_format), it.volumes.unknownIfZero(), it.chapters.unknownIfZero()))
        return SearchItem(
                it.id,
                if (it.isRanobe) Type.RANOBE else Type.MANGA,
                nameText,
                it.image,
                titleText.typeLabel(it.type).orEmpty()
        )
    }

    private fun convertAnime(it: Anime): SearchItem {
        val name = settings.titleName(it.name, it.nameRu)
        val aired = if (it.status == Status.RELEASED) it.episodes else it.episodesAired
        val nameText = name.plus(String.format(context.getString(R.string.episodes_format), aired, it.episodes.unknownIfZero()))
        return SearchItem(
                it.id,
                Type.ANIME,
                nameText,
                it.image,
                titleText.typeLabel(it.type).orEmpty()
        )
    }
}
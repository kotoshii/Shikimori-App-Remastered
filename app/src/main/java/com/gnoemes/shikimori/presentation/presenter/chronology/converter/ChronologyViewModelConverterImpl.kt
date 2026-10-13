package com.gnoemes.shikimori.presentation.presenter.chronology.converter

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.titleName
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.anime.domain.AnimeWithStatus
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.chronology.ChronologyItem
import com.gnoemes.shikimori.entity.chronology.ChronologyViewModel
import com.gnoemes.shikimori.entity.common.domain.RelationType
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.manga.domain.MangaWithStatus
import com.gnoemes.shikimori.presentation.presenter.common.converter.TitleTextConverter
import javax.inject.Inject

class ChronologyViewModelConverterImpl @Inject constructor(
        private val context: Context,
        private val settings: SettingsSource,
        private val titleText: TitleTextConverter
) : ChronologyViewModelConverter {

    override fun apply(t: List<ChronologyItem>, isGuest: Boolean): List<ChronologyViewModel> {
        return t.mapNotNull { convertNode(it, isGuest) }
    }

    private fun convertNode(it: ChronologyItem, isGuest: Boolean): ChronologyViewModel? = when {
        it.item is Anime -> convertAnime(AnimeWithStatus(it.item, it.rateId, it.rateStatus), it.relation, isGuest)
        it.item is Manga -> convertManga(MangaWithStatus(it.item, it.rateId, it.rateStatus), it.relation, isGuest)
        else -> null
    }

    private fun convertAnime(it: AnimeWithStatus, relationType: RelationType, isGuest: Boolean): ChronologyViewModel {
        return ChronologyViewModel(
                it.anime.id,
                it.rateId ?: Constants.NO_ID,
                it.anime.linkedType,
                settings.titleName(it.anime.name, it.anime.nameRu),
                titleText.describe(it.anime),
                it.anime.image,
                getRelation(relationType),
                it.status,
                isGuest
        )
    }

    private fun getRelation(relation: RelationType): String =
            when (relation) {
                RelationType.PREQUEL -> context.getString(R.string.chronology_prequel)
                RelationType.SEQUEL -> context.getString(R.string.chronology_sequel)
                RelationType.SUMMARY -> context.getString(R.string.chronology_summary)
                RelationType.FULL_STORY -> context.getString(R.string.chronology_parent_story)
                RelationType.PARENT_STORY -> context.getString(R.string.chronology_parent_story)
                RelationType.ALTERNATIVE_VERSION -> context.getString(R.string.chronology_alternative_version)
                RelationType.ALTERNATIVE_SETTING -> context.getString(R.string.chronology_alternative_setting)
                RelationType.SIDE_STORY -> context.getString(R.string.chronology_side_story)
                RelationType.OTHER -> context.getString(R.string.chronology_other)
                RelationType.NONE -> ""
            }

    private fun convertManga(it: MangaWithStatus, relationType: RelationType, isGuest: Boolean): ChronologyViewModel {
        return ChronologyViewModel(
                it.manga.id,
                it.rateId ?: Constants.NO_ID,
                it.manga.linkedType,
                settings.titleName(it.manga.name, it.manga.nameRu),
                titleText.describe(it.manga),
                it.manga.image,
                getRelation(relationType),
                it.status,
                isGuest
        )
    }
}

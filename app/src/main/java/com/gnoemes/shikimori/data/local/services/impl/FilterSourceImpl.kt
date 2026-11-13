package com.gnoemes.shikimori.data.local.services.impl

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.preference.UserSource
import com.gnoemes.shikimori.data.local.services.FilterSource
import com.gnoemes.shikimori.data.repository.common.GenreVocabularySource
import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.anime.domain.AnimeType
import com.gnoemes.shikimori.entity.common.domain.*
import com.gnoemes.shikimori.entity.manga.domain.MangaType
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.utils.date.DateTimeConverter
import com.gnoemes.shikimori.utils.labelRes
import com.gnoemes.shikimori.utils.nameRes
import java.util.Locale
import org.joda.time.DateTime
import javax.inject.Inject

class FilterSourceImpl @Inject constructor(
        private val context: Context,
        private val settingsSource: SettingsSource,
        private val userSource: UserSource,
        private val genreVocabulary: GenreVocabularySource,
        private val dateTimeConverter: DateTimeConverter
) : FilterSource {

    companion object {
        /**
         * Adult genres per vocabulary, hidden unless adult content is on. Erotica is 17+ rather
         * than adult and stays visible, as it always has - see GENRES_V2_SPIKE.md.
         */
        private val R18_ANIME = setOf(12L, 33L, 34L)    //Хентай, Яой, Юри
        private val R18_MANGA = setOf(602L, 65L, 75L)   //the same three, manga ids
    }

    override fun getAnimeFilters(): List<FilterCategory> = listOfNotNull(
            FilterCategory(FilterType.GENRE, getGenreString(), getFilters(FilterType.GENRE, Type.ANIME)),
            FilterCategory(FilterType.SEASON, getSeasonString(), getFilters(FilterType.SEASON, Type.ANIME)),
            FilterCategory(FilterType.STATUS, getStatusString(), getFilters(FilterType.STATUS, Type.ANIME)),
            rateCategory(Type.ANIME),
            FilterCategory(FilterType.KIND, getTypeString(), getFilters(FilterType.KIND, Type.ANIME)),
            FilterCategory(FilterType.DURATION, getDurationString(), getFilters(FilterType.DURATION, Type.ANIME)),
            FilterCategory(FilterType.AGE_RATING, getAgeRatingString(), getFilters(FilterType.AGE_RATING, Type.ANIME)),
            FilterCategory(FilterType.ORIGIN, context.getString(R.string.filter_origin), getFilters(FilterType.ORIGIN, Type.ANIME)),
            scoreCategory()
    )

    override fun getMangaFilters(): List<FilterCategory> = listOfNotNull(
            FilterCategory(FilterType.GENRE, getGenreString(), getFilters(FilterType.GENRE, Type.MANGA)),
            FilterCategory(FilterType.SEASON, getSeasonString(), getFilters(FilterType.SEASON, Type.MANGA)),
            FilterCategory(FilterType.STATUS, getStatusString(), getFilters(FilterType.STATUS, Type.MANGA)),
            rateCategory(Type.MANGA),
            FilterCategory(FilterType.KIND, getTypeString(), getFilters(FilterType.KIND, Type.MANGA)),
            scoreCategory()
    )

    override fun getRanobeFilters(): List<FilterCategory> = listOfNotNull(
            FilterCategory(FilterType.GENRE, getGenreString(), getFilters(FilterType.GENRE, Type.RANOBE)),
            FilterCategory(FilterType.SEASON, getSeasonString(), getFilters(FilterType.SEASON, Type.RANOBE)),
            FilterCategory(FilterType.STATUS, getStatusString(), getFilters(FilterType.STATUS, Type.RANOBE)),
            rateCategory(Type.RANOBE),
            FilterCategory(FilterType.KIND, getTypeString(), getFilters(FilterType.KIND, Type.RANOBE)),
            scoreCategory()
    )

    /**
     * «Оценка»: the api's score at least N, one choice («6+»…«9+», P10-T4 Q7). The api takes 1-9, but below
     * 6 a chip removes almost nothing beyond the titles with no score. The same for every type.
     */
    private fun scoreCategory(): FilterCategory =
            FilterCategory(FilterType.SCORE, context.getString(R.string.filter_score),
                    (6..9).map { convert(FilterType.SCORE.value, "$it", "$it+") }.toMutableList())

    //«Список» filters by the signed-in user's own list; a guest has none to filter by (BUGS #15)
    private fun rateCategory(type: Type): FilterCategory? =
            if (userSource.isSignedIn()) FilterCategory(FilterType.RATE, getRateStatusString(), getFilters(FilterType.RATE, type))
            else null

    override fun getAnimeSortFilters(): List<FilterItem> =
            getList(R.array.anime_search_sort)
                    .zip(getList(R.array.anime_search_sort_values))
                    .map { (name, value) -> FilterItem(SearchConstants.ORDER, value, name) }

    override fun getMangaSortFilters(): List<FilterItem> =
            getList(R.array.manga_search_sort)
                    .zip(getList(R.array.manga_search_sort_values))
                    .map { (name, value) -> FilterItem(SearchConstants.ORDER, value, name) }

    //the chips of one row for one catalog type; manga and ranobe differ only in «Тип»
    private fun getFilters(filter: FilterType, type: Type): MutableList<FilterItem> {
        val isAnime = type == Type.ANIME
        return when (filter) {
            FilterType.GENRE -> getGenres(isAnime)
            FilterType.SEASON -> getSeasons(isAnime)
            FilterType.STATUS -> getStatuses(isAnime)
            FilterType.RATE -> getRates(isAnime)
            FilterType.KIND -> getKinds(type)
            FilterType.DURATION -> getDurations()
            FilterType.AGE_RATING -> getAgeRatings()
            FilterType.ORIGIN -> getOrigins()
            else -> mutableListOf()
        }
    }

    private fun getAgeRatings(): MutableList<FilterItem> =
            getList(R.array.age_ratings)
                    .zip(AgeRating.values().asSequence().filter { it.rating != AgeRating.NONE.rating }.let { if (!settingsSource.allowR18Content) it.filter { !it.isR18 } else it }.map { it.rating.lowercase(Locale.getDefault()) }.toMutableList())
                    .map { (name, value) -> convert(FilterType.AGE_RATING.value, value, name) }
                    .toMutableList()

    private fun getDurations(): MutableList<FilterItem> =
            getList(R.array.duration)
                    .zip(getList(R.array.duration_names))
                    .map { (name, value) -> convert(FilterType.DURATION.value, value, name) }
                    .toMutableList()

    /**
     * The «Тип» chips, in the site's words and order: the same words as the cards. «TV: 12 серий»,
     * «TV: 24 серии», «TV: 48+ серий» narrow «TV Сериал» by its episode count, as on the site; they are
     * values of the api's `kind` filter, not kinds of a title. Ranobe has its own two kinds, «Ранобэ» and
     * «Новелла» (`/api/ranobe?kind=novel` works, though the docs do not list it; CATALOG.md §1).
     */
    private fun getKinds(type: Type): MutableList<FilterItem> {
        val kinds = when (type) {
            Type.ANIME -> listOf(
                    AnimeType.TV.type to AnimeType.TV.labelRes,
                    AnimeType.TV_13.type to R.string.type_tv_13,
                    AnimeType.TV_24.type to R.string.type_tv_24,
                    AnimeType.TV_48.type to R.string.type_tv_48
            ) + listOf(AnimeType.MOVIE, AnimeType.OVA, AnimeType.ONA, AnimeType.SPECIAL, AnimeType.TV_SPECIAL, AnimeType.MUSIC, AnimeType.PV, AnimeType.CM)
                    .map { it.type to it.labelRes }
            Type.RANOBE -> listOf(MangaType.LIGHT_NOVEL, MangaType.NOVEL)
                    .map { it.type to it.labelRes }
            else -> listOf(MangaType.MANGA, MangaType.MANHWA, MangaType.MANHUA, MangaType.ONE_SHOT, MangaType.DOUJIN)
                    .map { it.type to it.labelRes }
        }
        return kinds
                .mapNotNull { (value, label) -> label?.let { convert(FilterType.KIND.value, value, context.getString(it)) } }
                .toMutableList()
    }


    /**
     * The anime «Первоисточник» chips: the api's `origin` values in the site's words and order (live
     * 2026-11-13). The api also takes `unknown`, which the site does not offer. Anime only: manga and
     * ranobe answer 422 (CATALOG.md §1).
     */
    private fun getOrigins(): MutableList<FilterItem> = listOf(
            "original" to R.string.origin_original,
            "manga" to R.string.origin_manga,
            "web_manga" to R.string.origin_web_manga,
            "four_koma_manga" to R.string.origin_four_koma_manga,
            "light_novel" to R.string.origin_light_novel,
            "novel" to R.string.origin_novel,
            "web_novel" to R.string.origin_web_novel,
            "visual_novel" to R.string.origin_visual_novel,
            "game" to R.string.origin_game,
            "card_game" to R.string.origin_card_game,
            "book" to R.string.origin_book,
            "picture_book" to R.string.origin_picture_book,
            "radio" to R.string.origin_radio,
            "music" to R.string.origin_music,
            "mixed_media" to R.string.origin_mixed_media,
            "other" to R.string.origin_other
    )
            .map { (value, label) -> convert(FilterType.ORIGIN.value, value, context.getString(label)) }
            .toMutableList()

    private fun getRates(anime: Boolean): MutableList<FilterItem> =
            RateStatus.values()
                    .map { convert(FilterType.RATE.value, it.status.lowercase(Locale.getDefault()), context.getString(it.nameRes(anime))) }
                    .toMutableList()

    /**
     * The «Статус» chips, in the site's words and order for the type. Explicit pairs: the list used to
     * be the labels zipped with `Status.values()` by position, which a new status would have shifted
     * (BUGS #9). Paused and discontinued are for manga and ranobe only - the api answers anime's with
     * 422; «Недавно вышло»/«Недавно издано» is the api's `latest`, not a status.
     */
    private fun getStatuses(anime: Boolean): MutableList<FilterItem> {
        val statuses = if (anime) listOf(Status.ANONS, Status.ONGOING, Status.RELEASED)
        else listOf(Status.ANONS, Status.ONGOING, Status.RELEASED, Status.PAUSED, Status.DISCONTINUED)
        val items = statuses.map { convert(FilterType.STATUS.value, it.status, context.getString(it.labelRes(anime))) }
        val latest = convert(FilterType.STATUS.value, "latest", context.getString(if (anime) R.string.filters_latest_anime else R.string.filters_latest_manga))
        return (items.take(3) + latest + items.drop(3)).toMutableList()
    }

    /**
     * v2 genres, from the accumulated vocabulary. The old hardcoded genre list knew 48 genres where
     * v2 has 80, and none of the ones added since 2024.
     *
     * Adult genres are left out while adult content is off. Every other genre is offered, manga
     * and ranobe included: the catalog's rest `genre_v2` filters all of them.
     */
    private fun getGenres(anime: Boolean): MutableList<FilterItem> {
        val type = if (anime) GenreEntryType.ANIME else GenreEntryType.MANGA
        val adult = if (anime) R18_ANIME else R18_MANGA

        return genreVocabulary.genres(type)
                .asSequence()
                .filter { settingsSource.allowR18Content || it.id !in adult }
                .map { FilterItem(FilterType.GENRE.value, it.id.toString(), it.russianName, it.kind) }
                .toMutableList()
    }

    private fun convert(action: String, value: String?, text: String?) = FilterItem(action, value, text)

    private fun getDurationString(): String = context.getString(R.string.common_duration)
    private fun getStatusString(): String = context.getString(R.string.common_status)
    private fun getTypeString(): String = context.getString(R.string.common_type)
    private fun getGenreString(): String = context.getString(R.string.common_genre)
    private fun getRateStatusString(): String = context.getString(R.string.rate_status)
    private fun getAgeRatingString(): String = context.getString(R.string.common_age_rating)
    private fun getSeasonString(): String = context.getString(R.string.common_season)
    private fun getList(arrayRes: Int): MutableList<String> = context.resources.getStringArray(arrayRes).toMutableList()

    /**
     * The catalog's season list as the live site has it (checked 2026-10-13): for anime, every season
     * of next year, this year and last year; then each year from next year down to 2015, the decades
     * from the 2010s to the 1970s, and «До 1980» (`ancient`, which takes everything up to 1980). Manga
     * and ranobe get no seasons. 2015 and the decades are fixed on the site, not counted from today.
     */
    private fun getSeasons(isAnime: Boolean): MutableList<FilterItem> {
        val nextYear = DateTime.now().year + 1
        val items = mutableListOf<FilterItem>()

        if (isAnime) {
            val seasons = listOf(SearchConstants.SEASONS.WINTER, SearchConstants.SEASONS.SPRING, SearchConstants.SEASONS.SUMMER, SearchConstants.SEASONS.FALL)
            for (year in nextYear downTo nextYear - 2) {
                seasons.forEach {
                    val season = SeasonYear(it, year)
                    items.add(FilterItem(SearchConstants.SEASON, season.value, dateTimeConverter.convertSeasonToString(season)))
                }
            }
        }
        for (year in nextYear downTo 2015) {
            items.add(FilterItem(SearchConstants.SEASON, "$year", "$year"))
        }
        //201x is the 2010s: «10-е»
        for (decade in 201 downTo 197) {
            items.add(FilterItem(SearchConstants.SEASON, "${decade}x", context.getString(R.string.filter_decade, "${decade % 10}0")))
        }
        items.add(FilterItem(SearchConstants.SEASON, "ancient", context.getString(R.string.filter_older)))
        return items
    }

}
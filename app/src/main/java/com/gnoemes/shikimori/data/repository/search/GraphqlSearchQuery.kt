package com.gnoemes.shikimori.data.repository.search

import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.domain.SearchConstants

/**
 * Turns the query map the app already builds for REST (`SearchQueryBuilderImpl`) into a graphql
 * `animes(...)` / `mangas(...)` call.
 *
 * The **values** need no translation: graphql accepts the same comma separated ids and the same
 * `!` exclusion prefix REST does, verified against the live api on 2026-09-02 - `genre:"1,!4"`
 * returns what `genre=1,!4` returns. Only the argument *syntax* differs, which is what this does.
 */
object GraphqlSearchQuery {

    /**
     * Arguments graphql declares as a number, a boolean or an enum. Everything else is one of its
     * string scalars (`AnimeKindString`, `SeasonString`, ...) and has to be quoted.
     *
     * `order` is an `OrderEnum`, so it is written bare - quoting it is an
     * `argumentLiteralsIncompatible` error rather than an ignored argument.
     */
    private val UNQUOTED = setOf(
            SearchConstants.PAGE,
            SearchConstants.LIMIT,
            SearchConstants.CENSORED,
            SearchConstants.SCORE,
            SearchConstants.ORDER
    )

    /**
     * Arguments the app can send that graphql has no equivalent for. Sending one is an error that
     * fails the whole query, so they are dropped instead.
     *
     * `advanced` is a REST-only flag. Note `order=chapters` is **not** handled here: it was taken
     * out of the manga sort options instead, so the ui never offers a sort the api cannot do.
     */
    private val UNSUPPORTED = setOf(SearchConstants.ADVANCED)

    /**
     * Ranobe are mangas with a light novel or novel kind - graphql has no ranobe root query, and
     * this reproduces `/api/ranobe` exactly (verified against the live api).
     */
    private const val RANOBE_KIND = "light_novel,novel"

    fun build(type: GenreEntryType, queryMap: Map<String, String>, isRanobe: Boolean): String {
        val arguments = LinkedHashMap<String, String>()

        queryMap.forEach { (key, value) ->
            if (key in UNSUPPORTED || value.isEmpty()) return@forEach

            //shikimori cannot filter manga by most v2 genres; six of them have a v1 id that still
            //works and is swapped in here. See MangaGenreFilter
            val effective = if (type == GenreEntryType.MANGA && key == SearchConstants.GENRE) {
                MangaGenreFilter.mapArgument(value)
            } else value

            arguments[key] = if (key in UNQUOTED) effective else quote(effective)
        }

        //a ranobe search is a manga search narrowed to the two ranobe kinds. The ranobe filter
        //screen offers no kind of its own (FilterSourceImpl.getRanobeFilters), so nothing is
        //overwritten here - but were that to change, the narrowing has to win or the screen would
        //start listing manga
        if (isRanobe) arguments[SearchConstants.TYPE] = quote(RANOBE_KIND)

        val argumentList = arguments.entries.joinToString(",") { "${it.key}:${it.value}" }
        return "{${type.rootField}($argumentList){${fields(type)}}}"
    }

    private fun fields(type: GenreEntryType): String {
        val counters = when (type) {
            GenreEntryType.ANIME -> "episodes episodesAired"
            GenreEntryType.MANGA -> "volumes chapters"
        }

        //the poster is asked for in full because PosterResponse picks the rendition itself, jpeg
        //over webp - the app supports api 16 and webp with transparency needs 18
        return "id name russian url kind status score $counters " +
                "airedOn{date} releasedOn{date} " +
                "poster{originalUrl mainUrl mainAltUrl previewUrl previewAltUrl " +
                "mini2xUrl miniAlt2xUrl miniUrl miniAltUrl}"
    }

    /**
     * A graphql string literal. `search` carries whatever the user typed, so a quote or a backslash
     * in it would otherwise end the literal early and break the whole query.
     */
    private fun quote(value: String): String =
            "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

package com.gnoemes.shikimori.entity.common.data.graphql

import com.gnoemes.shikimori.entity.anime.domain.AnimeType
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.manga.domain.MangaType
import com.google.gson.annotations.SerializedName
import org.joda.time.DateTime

/**
 * Answer to a catalog search. Only the root field that was asked for is present.
 *
 * The fields mirror what `AnimeResponse` / `MangaResponse` carry, so the same domain models come
 * out the other side - see `GraphqlContentConverterImpl`. Ranobe have no root query of their own
 * and arrive as mangas.
 */
data class SearchQueryResponse(
        @field:SerializedName("data") val data: SearchQueryData?
)

data class SearchQueryData(
        @field:SerializedName("animes") val animes: List<GraphqlAnimeResponse>?,
        @field:SerializedName("mangas") val mangas: List<GraphqlMangaResponse>?
)

/**
 * `airedOn` and `releasedOn` are an `IncompleteDate`, not a string: shikimori keeps partial dates
 * (a year with no month) and [date] is null for those, while `year` still holds something. The app
 * only ever showed a full date, so only [date] is read - the same information the REST `aired_on`
 * string carried.
 */
data class GraphqlDate(
        @field:SerializedName("date") val date: DateTime?
)

data class GraphqlAnimeResponse(
        @field:SerializedName("id") val id: String?,
        @field:SerializedName("name") val name: String?,
        @field:SerializedName("russian") val nameRu: String?,
        @field:SerializedName("url") val url: String?,
        @field:SerializedName("poster") val poster: PosterResponse?,
        @field:SerializedName("kind") private val _type: AnimeType?,
        @field:SerializedName("status") private val _status: Status?,
        @field:SerializedName("score") val score: Double?,
        @field:SerializedName("episodes") val episodes: Int?,
        @field:SerializedName("episodesAired") val episodesAired: Int?,
        @field:SerializedName("airedOn") val dateAired: GraphqlDate?,
        @field:SerializedName("releasedOn") val dateReleased: GraphqlDate?
) {
    val status: Status
        get() = _status ?: Status.NONE

    val type: AnimeType
        get() = _type ?: AnimeType.NONE
}

/**
 * ⚠️ Manga statuses in graphql include `paused` and `discontinued`, which [Status] does not
 * declare. Gson leaves the field null for those and [status] falls back to `NONE`, exactly as the
 * REST response does for a status it does not know.
 */
data class GraphqlMangaResponse(
        @field:SerializedName("id") val id: String?,
        @field:SerializedName("name") val name: String?,
        @field:SerializedName("russian") val nameRu: String?,
        @field:SerializedName("url") val url: String?,
        @field:SerializedName("poster") val poster: PosterResponse?,
        @field:SerializedName("kind") private val _type: MangaType?,
        @field:SerializedName("status") private val _status: Status?,
        @field:SerializedName("score") val score: Double?,
        @field:SerializedName("volumes") val volumes: Int?,
        @field:SerializedName("chapters") val chapters: Int?,
        @field:SerializedName("airedOn") val dateAired: GraphqlDate?,
        @field:SerializedName("releasedOn") val dateReleased: GraphqlDate?
) {
    val status: Status
        get() = _status ?: Status.NONE

    val type: MangaType
        get() = _type ?: MangaType.UNKNOWN
}

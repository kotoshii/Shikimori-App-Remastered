package com.gnoemes.shikimori.domain.search

import androidx.collection.ArrayMap
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.SearchConstants
import io.reactivex.Single
import javax.inject.Inject

class SearchQueryBuilderImpl @Inject constructor(
        private val settingsSource: SettingsSource
) : SearchQueryBuilder {

    companion object {
        private const val DIVIDER = ","
    }

    override fun createQueryFromFilters(filters: Map<String, MutableList<FilterItem>>?, page: Int?, limit: Int?): Single<Map<String, String>> =
            Single.fromCallable<Map<String, String>> {
                val queryMap = ArrayMap<String, String>()
                filters?.forEach { entry -> queryMap[entry.key] = convertFilters(entry.value) }
                queryMap.putPageAndCensored(page, limit)
                queryMap
            }

    //a studio or genre opened from a title page, or a search, goes out sorted too (BUGS #12). Only the
    //title lists: character and people search read nothing but the text
    override fun createTitleListQuery(filters: Map<String, MutableList<FilterItem>>?, page: Int, limit: Int): Single<Map<String, String>> =
            createQueryFromFilters(filters, page, limit)
                    .map { query ->
                        if (query.containsKey(SearchConstants.ORDER)) query
                        else query + (SearchConstants.ORDER to SearchConstants.DEFAULT_ORDER.toString())
                    }

    override fun createQueryFromIds(ids: MutableCollection<Long>): Single<Map<String, String>> =
            Single.fromCallable<Map<String, String>> {
                val queryMap = ArrayMap<String, String>()
                if (ids.isNotEmpty()) {
                    queryMap[SearchConstants.IDS] = convertQuery(ids.map { it.toString() })
                    queryMap.putPageAndCensored(1, Constants.MAX_LIMIT)
                }
                queryMap
            }

    /** The page, the page size and the 18+ choice, which every catalog request carries. */
    private fun MutableMap<String, String>.putPageAndCensored(page: Int?, limit: Int?) {
        if (page != null) this[SearchConstants.PAGE] = page.toString()
        if (limit != null) this[SearchConstants.LIMIT] = limit.toString()
        this[SearchConstants.CENSORED] = (!settingsSource.allowR18Content).toString()
    }

    private fun convertFilters(values: MutableList<FilterItem>): String {
        return convertQuery(values.mapNotNull { it.value })
    }

    private fun convertQuery(values: List<String>): String {
        return if (!values.isNullOrEmpty()) {
            val builder = StringBuilder()
            values.forEach { builder.append(it).append(DIVIDER) }
            builder.replace(builder.lastIndexOf(DIVIDER), builder.length, "")
            builder.toString()
        } else ""
    }
}
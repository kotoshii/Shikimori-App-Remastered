package com.gnoemes.shikimori.presentation.presenter.rates

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.RateSort
import com.gnoemes.shikimori.entity.rates.domain.Rate

/**
 * The list's order: the [rates] whose name, in either language, holds [query], sorted by [sort].
 * Plain, so the list runs it off the main thread. [titleName] is the naming setting's choice of
 * name, for the name sort.
 */
fun sortAndSearchRates(rates: List<Rate>, sort: RateSort, descending: Boolean, query: String?,
                       titleName: (String, String?) -> String): List<Rate> {
    val found = if (query.isNullOrBlank()) rates else rates.filter { it.matches(query) }
    //each key once, not twice per comparison
    val keyed = found.map { it to it.sortKey(sort, titleName) }
    val order =
            if (descending) compareByDescending<Pair<Rate, Comparable<*>?>> { it.second }
            else compareBy { it.second }
    return keyed.sortedWith(order).map { it.first }
}

private fun Rate.matches(query: String): Boolean =
        if (type == Type.ANIME) anime?.name?.contains(query, ignoreCase = true) ?: false || anime?.nameRu?.contains(query, ignoreCase = true) ?: false
        else manga?.name?.contains(query, ignoreCase = true) ?: false || manga?.nameRu?.contains(query, ignoreCase = true) ?: false

private fun Rate.sortKey(sort: RateSort, titleName: (String, String?) -> String): Comparable<*>? {
    val isAnime = type == Type.ANIME
    return when (sort) {
        is RateSort.Id -> if (isAnime) anime?.id else manga?.id
        is RateSort.DateAired -> if (isAnime) anime?.dateAired else manga?.dateAired
        is RateSort.DateCreated -> createdDateTime
        is RateSort.DateUpdated -> updatedDateTime
        is RateSort.Episodes -> if (isAnime) anime?.episodes else manga?.chapters
        is RateSort.Progress -> if (isAnime) episodes else chapters
        is RateSort.Score -> score
        is RateSort.Name ->
            if (isAnime) anime?.let { titleName(it.name, it.nameRu) }
            else manga?.let { titleName(it.name, it.nameRu) }
    }
}

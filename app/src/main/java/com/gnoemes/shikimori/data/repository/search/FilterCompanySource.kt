package com.gnoemes.shikimori.data.repository.search

import com.gnoemes.shikimori.data.network.AnimeApi
import com.gnoemes.shikimori.data.network.MangaApi
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.search.domain.FilterType
import io.reactivex.Single
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The filter sheet's long lists: every studio (`/api/studios`, about 1900) and every publisher
 * (`/api/publishers`, about 1550), as chips to pick, by name. Each is loaded when its panel first opens
 * and kept while the app runs: the api has no search or paging for them, and they change rarely
 * (CATALOG.md §7). A failed load is not kept, so the next opening asks again. Names are trimmed: a few
 * studios have a space before or after (` Geijutsu Eigasha` came first in the list).
 */
@Singleton
class FilterCompanySource @Inject constructor(
        private val animeApi: AnimeApi,
        private val mangaApi: MangaApi
) {

    @Volatile
    private var studios: List<FilterItem>? = null

    @Volatile
    private var publishers: List<FilterItem>? = null

    /** The studios for [FilterType.STUDIO], the publishers for [FilterType.PUBLISHER]. */
    fun get(filter: FilterType): Single<List<FilterItem>> = when (filter) {
        FilterType.STUDIO -> studios?.let { Single.just(it) }
                ?: animeApi.getStudios()
                        .map { list -> sorted(list.map { FilterItem(filter.value, it.id.toString(), it.name.trim()) }) }
                        .doOnSuccess { studios = it }
        FilterType.PUBLISHER -> publishers?.let { Single.just(it) }
                ?: mangaApi.getPublishers()
                        .map { list -> sorted(list.map { FilterItem(filter.value, it.id.toString(), it.name.trim()) }) }
                        .doOnSuccess { publishers = it }
        else -> Single.just(emptyList())
    }

    /** The list when it is kept already, else null: a panel shows it at once, with no spinner (P10-T4 Q16). */
    fun kept(filter: FilterType): List<FilterItem>? = when (filter) {
        FilterType.STUDIO -> studios
        FilterType.PUBLISHER -> publishers
        else -> null
    }

    private fun sorted(items: List<FilterItem>): List<FilterItem> =
            items.sortedBy { it.localizedText?.lowercase(Locale.ROOT) }
}

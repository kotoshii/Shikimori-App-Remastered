package com.gnoemes.shikimori.domain.chronology

import com.gnoemes.shikimori.data.repository.anime.AnimeRepository
import com.gnoemes.shikimori.data.repository.manga.MangaRepository
import com.gnoemes.shikimori.data.repository.rates.RatesRepository
import com.gnoemes.shikimori.data.repository.search.SearchRepository
import com.gnoemes.shikimori.domain.search.SearchQueryBuilder
import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.chronology.ChronologyItem
import com.gnoemes.shikimori.entity.chronology.ChronologyType
import com.gnoemes.shikimori.entity.common.domain.*
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

class ChronologyInteratorImpl @Inject constructor(
        private val animeRepository: AnimeRepository,
        private val mangaRepository: MangaRepository,
        private val rateRepository: RatesRepository,
        private val searchRepository: SearchRepository,
        private val searchQueryBuilder: SearchQueryBuilder
) : ChronologyInteractor {

    companion object {
        private val CHAIN = arrayOf(RelationType.SEQUEL, RelationType.PREQUEL)
    }

    override fun getItems(id: Long, type: Type, chronologyType: ChronologyType): Single<List<ChronologyItem>> =
            (if (type == Type.ANIME) animeRepository.getFranchise(id) else mangaRepository.getFranchise(id, type))
                    .flatMap { loadItems(id, it, type, chronologyType) }
                    .applyErrorHandlerAndSchedulers()

    /** The franchise's titles, fetched by id in chunks, and the user's rates, joined into the list. */
    private fun loadItems(id: Long, franchise: Franchise, type: Type, chronologyType: ChronologyType): Single<List<ChronologyItem>> {
        if (franchise.nodes.isEmpty()) {
            return Single.just(emptyList())
        }

        val singles = franchise.nodes
                .map { node -> node.id }
                .chunked(Constants.MAX_ANIME_LIMIT)
                .map { ids -> searchQueryBuilder.createQueryFromIds(ids.toMutableList()).flatMap { query -> searchRepository.getList(type, query) } }

        return Single.zip(
                Single.zip(singles) { results -> results.flatMap { it as List<LinkedContent> } },
                rateRepository.getMyRates(type),
                BiFunction { titles: List<LinkedContent>, rates: Map<Long, UserRate> ->
                    buildItems(selectEntries(franchise, id, chronologyType), titles, rates)
                }
        )
    }

    /**
     * Which titles the list shows, in order, each with its relation to [id]. [chronologyType] only
     * applies to a title that is in a sequel/prequel chain; any other title shows its own relations.
     */
    private fun selectEntries(franchise: Franchise, id: Long, chronologyType: ChronologyType): List<Pair<Long, RelationType>> {
        val byWeight = franchise.relations.sortedByDescending { it.weight }
        val entry = byWeight.find { it.targetId == id && it.relation in CHAIN }
                ?: return byWeight.filter { it.sourceId == id }.map { it.targetId to it.relation }

        return when (chronologyType) {
            ChronologyType.MAIN -> getRootBranch(franchise.relations, findChainStart(franchise.relations, entry)).map { it.targetId to it.relation }
            ChronologyType.LINKED_DIRECTLY -> franchise.relations.filter { it.sourceId == id && it.relation !in CHAIN }.map { it.targetId to it.relation }
            ChronologyType.ALL -> franchise.nodes.map { it.id to RelationType.NONE }
        }
    }

    /** Walks the sequel links back from [entry] to the start of its chain. */
    private fun findChainStart(relations: List<FranchiseRelation>, entry: FranchiseRelation): FranchiseRelation {
        var current = entry
        val sequels = relations.filter { it.relation == RelationType.SEQUEL }.toMutableList()

        while (sequels.isNotEmpty()) {
            val next = sequels.find { it.targetId == current.sourceId }
            sequels.remove(current)
            if (next == null) break
            current = next
        }
        return current
    }

    /** One item per selected title that was found, with the user's rate; then by start date. */
    private fun buildItems(entries: List<Pair<Long, RelationType>>, titles: List<LinkedContent>, rates: Map<Long, UserRate>): List<ChronologyItem> =
            entries
                    .mapNotNull { (titleId, relation) ->
                        val title = titles.find { it.linkedId == titleId } ?: return@mapNotNull null
                        val rate = rates[title.linkedId]
                        ChronologyItem(title.linkedId, rate?.id, title, relation, rate?.status)
                    }
                    .sortedBy {
                        when (it.item) {
                            is Anime -> it.item.dateAired?.millis
                            is Manga -> it.item.dateAired?.millis
                            else -> null
                        }
                    }

    private fun getRootBranch(relations: List<FranchiseRelation>, root: FranchiseRelation): List<FranchiseRelation> {
        val rootBranch = mutableListOf<FranchiseRelation>()

        val rootSelf = relations.find { it.targetId == root.sourceId && it.relation == RelationType.PREQUEL }
        if (rootSelf != null) rootBranch.add(rootSelf)

        var current = root
        val relatedNodes = relations
                .sortedByDescending { it.weight }
                .filter { it.relation == RelationType.SEQUEL }
                .toMutableList()

        while (relatedNodes.isNotEmpty()) {
            val next = relatedNodes.find { it.sourceId == current.targetId }
            relatedNodes.remove(current)
            rootBranch.add(current)
            if (next == null) break
            current = next.copy()
        }
        return rootBranch
    }
}

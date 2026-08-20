package com.gnoemes.shikimori.data.repository.common.impl

import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.network.GraphqlApi
import com.gnoemes.shikimori.data.repository.common.PosterSource
import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlRequest
import com.gnoemes.shikimori.entity.common.data.graphql.PosterEntityType
import com.gnoemes.shikimori.entity.common.data.graphql.PosterHolder
import com.gnoemes.shikimori.entity.common.data.graphql.PosterKey
import com.gnoemes.shikimori.entity.common.data.graphql.PosterResponse
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PosterSourceImpl @Inject constructor(
        private val api: GraphqlApi,
        private val settingsSource: SettingsSource
) : PosterSource {

    /**
     * Access ordered LRU - scrolling a long list only ever asks for ids it has not seen yet.
     * Reached from OkHttp worker threads, hence the synchronized wrapper.
     */
    private val cache: MutableMap<PosterKey, PosterResponse> = Collections.synchronizedMap(
            object : LinkedHashMap<PosterKey, PosterResponse>(INITIAL_CAPACITY, LOAD_FACTOR, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<PosterKey, PosterResponse>?): Boolean =
                        size > CACHE_SIZE
            }
    )

    /** Whatever `allowR18Content` was when the cache was filled, see [invalidateIfR18SettingChanged]. */
    private var cachedWithR18: Boolean? = null

    /**
     * Set when shikimori answers 429, to stay out of the way for [COOLDOWN_NANOS].
     *
     * Deliberately short: `ShikimoriRateLimiter` already keeps the whole app inside the api's
     * budget, so a 429 here means something unusual and recovering quickly matters more than
     * backing off hard - a long pause just leaves placeholders on screen for no reason.
     */
    @Volatile
    private var pausedUntilNanos = System.nanoTime()

    override fun onRateLimited() {
        pausedUntilNanos = System.nanoTime() + COOLDOWN_NANOS
    }

    override fun resolve(keys: Collection<PosterKey>): Map<PosterKey, PosterResponse> {
        if (keys.isEmpty()) return emptyMap()

        val allowR18 = settingsSource.allowR18Content
        invalidateIfR18SettingChanged(allowR18)

        val resolved = HashMap<PosterKey, PosterResponse>(keys.size)
        val missing = LinkedHashSet<PosterKey>()

        keys.forEach { key ->
            val cached = cache.get(key)
            when {
                cached == null -> missing.add(key)
                //null poster already confirmed by the server, do not ask again
                cached !== PosterResponse.EMPTY -> resolved[key] = cached
            }
        }

        //while throttled, answer from the cache only - a placeholder beats a failing screen
        if (missing.isNotEmpty() && !isPaused()) fetch(missing, resolved, allowR18)
        return resolved
    }

    private fun isPaused(): Boolean = System.nanoTime() - pausedUntilNanos < 0

    /**
     * Toggling "adult content" in the settings changes what the queries may answer, so both the
     * cached posters and the cached "no poster" answers stop being trustworthy.
     */
    private fun invalidateIfR18SettingChanged(allowR18: Boolean) {
        synchronized(cache) {
            if (cachedWithR18 != allowR18) {
                cache.clear()
                cachedWithR18 = allowR18
            }
        }
    }

    private fun fetch(
            keys: Collection<PosterKey>,
            out: MutableMap<PosterKey, PosterResponse>,
            allowR18: Boolean
    ) {
        val chunksByType = keys
                .groupBy { it.type }
                .mapValues { entry -> entry.value.map { it.id }.distinct().chunked(MAX_IDS_PER_QUERY) }

        val batchCount = chunksByType.values.map { it.size }.max() ?: 0

        for (index in 0 until batchCount) {
            if (isPaused()) return
            val batch = LinkedHashMap<PosterEntityType, List<Long>>()
            chunksByType.forEach { (type, chunks) -> chunks.getOrNull(index)?.let { batch[type] = it } }
            if (batch.isNotEmpty()) executeBatch(batch, out, allowR18)
        }
    }

    private fun executeBatch(
            batch: Map<PosterEntityType, List<Long>>,
            out: MutableMap<PosterKey, PosterResponse>,
            allowR18: Boolean
    ) {
        //ShikimoriRateLimiter queues this behind whatever the screens are already asking for
        val data = try {
            val response = api.getPosters(GraphqlRequest(buildQuery(batch, allowR18))).execute()
            if (response.code() == HTTP_TOO_MANY_REQUESTS) onRateLimited()
            response.body()?.data
        } catch (e: Throwable) {
            //network is best effort here - the placeholder simply stays in place
            null
        }

        if (data == null) return

        batch.forEach { (type, ids) ->
            val holders = when (type) {
                PosterEntityType.ANIME -> data.animes
                PosterEntityType.MANGA -> data.mangas
                PosterEntityType.CHARACTER -> data.characters
                PosterEntityType.PERSON -> data.people
            }
            apply(type, ids, holders, out)
        }
    }

    private fun apply(
            type: PosterEntityType,
            requestedIds: List<Long>,
            holders: List<PosterHolder>?,
            out: MutableMap<PosterKey, PosterResponse>
    ) {
        val answered = HashSet<Long>()

        holders?.forEach { holder ->
            val id = holder.id?.toLongOrNull() ?: return@forEach
            answered.add(id)
            val poster = holder.poster ?: return@forEach
            val key = PosterKey(type, id)
            cache.put(key, poster)
            out[key] = poster
        }

        //remember the negative answers too, otherwise every page reload asks again
        requestedIds.forEach { id ->
            if (!answered.contains(id)) cache.put(PosterKey(type, id), PosterResponse.EMPTY)
        }
    }

    /**
     * Builds a single query asking for every type of the batch at once, e.g.
     * `{animes(ids:"1,2",limit:50,censored:true){...} people(ids:["3"],limit:50){...}}`
     *
     * `limit` is mandatory - it defaults to 2 and would silently truncate the answer.
     *
     * `censored` mirrors what the rest of the app sends, see `SearchQueryBuilderImpl`: with adult
     * content turned off the query refuses to answer for hentai/yaoi/yuri, so those keep their
     * placeholder. Characters and people have no such filter.
     */
    private fun buildQuery(batch: Map<PosterEntityType, List<Long>>, allowR18: Boolean): String {
        val builder = StringBuilder("{")
        val censored = !allowR18

        batch.forEach { (type, ids) ->
            when (type) {
                PosterEntityType.ANIME ->
                    builder.append("animes(ids:\"${ids.joinToString(",")}\",limit:$MAX_IDS_PER_QUERY,censored:$censored)")
                PosterEntityType.MANGA ->
                    builder.append("mangas(ids:\"${ids.joinToString(",")}\",limit:$MAX_IDS_PER_QUERY,censored:$censored)")
                PosterEntityType.CHARACTER ->
                    builder.append("characters(ids:\"${ids.joinToString(",")}\",limit:$MAX_IDS_PER_QUERY)")
                //people is the only root query taking a list of ids instead of a comma separated string
                PosterEntityType.PERSON ->
                    builder.append("people(ids:[${ids.joinToString(",") { "\"$it\"" }}],limit:$MAX_IDS_PER_QUERY)")
            }
            builder.append(POSTER_FIELDS)
        }

        return builder.append("}").toString()
    }

    companion object {
        private const val CACHE_SIZE = 1024
        private const val INITIAL_CAPACITY = 64
        private const val LOAD_FACTOR = 0.75f
        private const val MAX_IDS_PER_QUERY = 50
        private const val HTTP_TOO_MANY_REQUESTS = 429

        /** Short enough that a single stray 429 cannot keep placeholders on screen. */
        private val COOLDOWN_NANOS = TimeUnit.SECONDS.toNanos(5)

        private const val POSTER_FIELDS =
                "{id poster{originalUrl mainUrl mainAltUrl previewUrl previewAltUrl " +
                        "mini2xUrl miniAlt2xUrl miniUrl miniAltUrl}}"
    }
}

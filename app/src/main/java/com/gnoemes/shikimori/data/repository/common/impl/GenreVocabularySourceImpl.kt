package com.gnoemes.shikimori.data.repository.common.impl

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.network.GraphqlApi
import com.gnoemes.shikimori.data.repository.common.GenreVocabularySource
import com.gnoemes.shikimori.entity.common.data.graphql.GenreEntryType
import com.gnoemes.shikimori.entity.common.data.graphql.GenreResponseV2
import com.gnoemes.shikimori.entity.common.data.graphql.GraphqlRequest
import com.gnoemes.shikimori.entity.common.domain.GenreV2
import io.reactivex.Completable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenreVocabularySourceImpl @Inject constructor(
        private val context: Context,
        private val api: GraphqlApi,
        private val settingsSource: SettingsSource
) : GenreVocabularySource {

    override fun genres(type: GenreEntryType): List<GenreV2> {
        val stored = decode(read(type))
        val known = if (stored.isEmpty()) seed(type).also { write(type, encode(it)) } else stored

        //plain string order, which is by code unit: latin labels come before cyrillic ones, so an
        //acronym like CGDCT leads its section. Chosen by the user over sorting it to the end.
        //Deliberately not a Collator either - the order must not change with the phone's locale
        return known.sortedWith(compareBy({ it.kind.ordinal }, { it.russianName }))
    }

    /**
     * One request for both vocabularies, using aliases - they are always wanted together, and the
     * rate limiter treats a second call as a second departure slot.
     */
    override fun refresh(): Completable = api
            .getGenreVocabulary(GraphqlRequest(
                    "{anime: genres(entryType: Anime){id name russian kind}" +
                            " manga: genres(entryType: Manga){id name russian kind}}"))
            .doOnSuccess { response ->
                merge(GenreEntryType.ANIME, response.data?.anime)
                merge(GenreEntryType.MANGA, response.data?.manga)
            }
            .ignoreElement()
            //the stored vocabulary is what the filter screen reads, and it is already populated -
            //a failed refresh must leave the app exactly as it was, not surface an error
            .onErrorComplete()

    /**
     * Adds what is new, updates the labels of what is known, and **keeps what is missing**.
     *
     * Updating labels matters as much as adding: it is how a renamed genre stays correct, and
     * shikimori has reused ids for different genres before (39 was Police, it is now Detective),
     * so whatever the api says about an id it *does* return is taken as the truth.
     */
    private fun merge(type: GenreEntryType, fetched: List<GenreResponseV2>?) {
        val incoming = convert(fetched)
        if (incoming.isEmpty()) return

        val merged = LinkedHashMap<Long, GenreV2>()
        genres(type).forEach { merged[it.id] = it }
        incoming.forEach { merged[it.id] = it }

        write(type, encode(merged.values))
    }

    private fun convert(fetched: List<GenreResponseV2>?): List<GenreV2> = fetched.orEmpty()
            .mapNotNull { genre ->
                val id = genre.id?.toLongOrNull() ?: return@mapNotNull null
                val russian = genre.russian?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

                GenreV2(id, genre.name.orEmpty(), russian, GenreV2.Kind.of(genre.kind))
            }

    /** The bundled snapshot, read once when the store is empty. */
    private fun seed(type: GenreEntryType): List<GenreV2> = try {
        val json = context.resources.openRawResource(R.raw.genres_v2_seed)
                .bufferedReader()
                .use { it.readText() }
        val root = org.json.JSONObject(json)
        val array = root.getJSONArray(if (type == GenreEntryType.ANIME) "anime" else "manga")

        (0 until array.length()).mapNotNull { index ->
            val item = array.getJSONObject(index)
            val russian = item.optString("russian").takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

            GenreV2(
                    item.getLong("id"),
                    item.optString("name"),
                    russian,
                    GenreV2.Kind.of(item.optString("kind"))
            )
        }
    } catch (e: Exception) {
        //a broken seed must not take the filter screen down - an empty vocabulary only means the
        //genre category has nothing in it until the first refresh lands
        emptyList()
    }

    private fun read(type: GenreEntryType): Set<String> = when (type) {
        GenreEntryType.ANIME -> settingsSource.animeGenres
        GenreEntryType.MANGA -> settingsSource.mangaGenres
    }

    private fun write(type: GenreEntryType, value: Set<String>) = when (type) {
        GenreEntryType.ANIME -> settingsSource.animeGenres = value
        GenreEntryType.MANGA -> settingsSource.mangaGenres = value
    }

    /**
     * `id|kind|name|russian`, one genre per entry, following the plain string sets the hosting
     * filter already stores. The russian label is last so a `|` inside it survives the split.
     */
    private fun encode(genres: Collection<GenreV2>): Set<String> = genres
            .map { "${it.id}|${it.kind.name}|${it.name}|${it.russianName}" }
            .toCollection(LinkedHashSet())

    private fun decode(stored: Set<String>): List<GenreV2> = stored.mapNotNull { entry ->
        val parts = entry.split("|", limit = 4)
        if (parts.size < 4) return@mapNotNull null

        val id = parts[0].toLongOrNull() ?: return@mapNotNull null
        //a label-less genre would draw as a blank chip nobody can identify, so it is dropped here
        //as well as on the way in - this is the last gate before the filter screen
        if (parts[3].isBlank()) return@mapNotNull null

        val kind = try {
            GenreV2.Kind.valueOf(parts[1])
        } catch (e: IllegalArgumentException) {
            GenreV2.Kind.UNKNOWN
        }

        GenreV2(id, parts[2], parts[3], kind)
    }
}

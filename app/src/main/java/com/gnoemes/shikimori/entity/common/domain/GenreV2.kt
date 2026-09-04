package com.gnoemes.shikimori.entity.common.domain

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * A genre as shikimori's v2 taxonomy describes it - the graphql `Genre` type.
 *
 * Replaces the hardcoded [Genre] enum for everything the user sees. v2 splits one flat list into
 * three [Kind]s, and unlike the rest api it is populated for titles added from 2025 onward, where
 * `/api/animes/{id}` returns an empty `genres` array. See docs/_internal/GENRES_V2_SPIKE.md.
 *
 * ⚠️ [id] only means something together with the entry type it was fetched for. Anime and manga
 * ids are separate namespaces, and five ids mean **different genres** in v1 and v2 (39 was Police,
 * it is now Detective), so an id must never be carried across vocabularies.
 */
@Parcelize
data class GenreV2(
        val id: Long,
        val name: String,
        val russianName: String,
        val kind: Kind
) : Parcelable {

    /**
     * Declaration order is display order - demographic first, the long tail of themes last.
     * See the details chips in `AnimeDetailsViewModelConverterImpl`.
     */
    enum class Kind {
        DEMOGRAPHIC, GENRE, THEME, UNKNOWN;

        companion object {

            /**
             * [UNKNOWN] rather than null for a kind shikimori adds later: an unrecognised kind
             * still renders as an ordinary chip instead of the genre disappearing, which is how
             * the v1 converter lost every genre it did not know.
             */
            fun of(raw: String?): Kind = when (raw?.toLowerCase()) {
                "demographic" -> DEMOGRAPHIC
                "genre" -> GENRE
                "theme" -> THEME
                else -> UNKNOWN
            }
        }
    }
}

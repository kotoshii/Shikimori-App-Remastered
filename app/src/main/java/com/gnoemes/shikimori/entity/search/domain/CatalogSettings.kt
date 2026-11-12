package com.gnoemes.shikimori.entity.search.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * How «Поиск» shows its list, set in its «Отображение» sheet (P10-T3): one set for every type, all on
 * by default. Each part of a card can be hidden; on a character or a person only the picture and the
 * name are ever shown.
 */
@Parcelize
data class CatalogSettings(
        /**
         * The cards' size, as the count of them in a row on a phone held upright, [MIN_COLUMNS]..[MAX_COLUMNS]
         * («Крупные», «Средние», «Мелкие»): a wider screen fits more cards of that size (P10-T3 Q11).
         */
        val columns: Int = MIN_COLUMNS,
        /** The kind on the poster («TV Сериал», «Манга»). */
        val showType: Boolean = true,
        /** The user's list status, a badge on the poster. */
        val showStatus: Boolean = true,
        /** The episodes or chapters, on the line under the name. */
        val showCount: Boolean = true,
        /** The score, a chip on the poster. */
        val showScore: Boolean = true,
        /** The name under the poster. */
        val showName: Boolean = true,
        /** The start year, or «Онгоинг»/«Анонс», on the line under the name. */
        val showYear: Boolean = true
) : Parcelable {

    companion object {
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 4
    }
}

package com.gnoemes.shikimori.entity.search.presentation

import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.RateStatus

/**
 * A catalog card as it is drawn (P10-T3): a null part is not on this card - hidden in «Отображение»
 * (CatalogSettings), or not one a character or a person has.
 */
data class SearchItem(
        val id: Long,
        val type: Type,
        val image: Image,
        /** The name under the picture. */
        val name: String?,
        /** The kind on the poster («TV Сериал»). */
        val typeText: String?,
        /** The line under the name («2019  •  24 эп.»); empty when the title has nothing for it, so the card keeps its height. */
        val description: CharSequence?,
        /** The score on the poster's chip. */
        val score: String?,
        /** The user's list status, the badge on the poster. */
        val status: RateStatus?
)

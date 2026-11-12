package com.gnoemes.shikimori.presentation.view.search.adapter

import kotlin.math.roundToInt

/**
 * The sizes on a catalog card, one look for every card of the grid (P10-T3). Three steps by the card's
 * width, named by how many cards a phone held upright shows in a row: a wider screen with cards of the
 * same width draws them the same. The first sizes were the mockup's (scaled as the site's chip); set again
 * after the user's check on a device, 2026-11-12: bigger at 2 and 3 in a row, 4 kept.
 */
data class CatalogCardLook(
        /** The kind's text, sp. */
        val typeSp: Float,
        /** The score chip's text, sp. */
        val scoreSp: Float,
        /** The status badge, dp. */
        val badgeDp: Int,
        /** The name's and the line's text, sp. */
        val nameSp: Float,
        val lineSp: Float
) {

    /**
     * The badge's distance from the poster's corner, and its icon, dp: the calendar's badge (a 32dp circle, its
     * 24dp icon, 8dp from the corner), scaled to the badge on smaller cards.
     */
    val badgeInsetDp: Int get() = (badgeDp * 0.25f).roundToInt()
    val badgeIconDp: Int get() = (badgeDp * 0.75f).roundToInt()

    companion object {
        //2 in a row: 160dp on a 360dp phone, 185dp on a 411dp one; 3: 104 and 121dp; 4: 76 and 89dp
        private const val LARGE_FROM_DP = 145f
        private const val MEDIUM_FROM_DP = 100f

        /** 2 in a row: the old card's 14sp kind and the calendar's 32dp badge. */
        val LARGE = CatalogCardLook(typeSp = 14f, scoreSp = 12f, badgeDp = 32, nameSp = 13f, lineSp = 11f)
        /** 3 in a row. */
        val MEDIUM = CatalogCardLook(typeSp = 11f, scoreSp = 11f, badgeDp = 24, nameSp = 12f, lineSp = 10f)
        /** 4 in a row. */
        val SMALL = CatalogCardLook(typeSp = 9f, scoreSp = 9f, badgeDp = 18, nameSp = 11f, lineSp = 9f)

        fun of(cardWidthDp: Float): CatalogCardLook = when {
            cardWidthDp >= LARGE_FROM_DP -> LARGE
            cardWidthDp >= MEDIUM_FROM_DP -> MEDIUM
            else -> SMALL
        }
    }
}

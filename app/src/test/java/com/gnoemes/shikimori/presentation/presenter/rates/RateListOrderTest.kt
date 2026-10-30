package com.gnoemes.shikimori.presentation.presenter.rates

import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.anime.domain.AnimeType
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.Status
import com.gnoemes.shikimori.entity.common.presentation.RateSort
import com.gnoemes.shikimori.entity.rates.domain.Rate
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class RateListOrderTest {

    private val english: (String, String?) -> String = { name, _ -> name }
    private val russian: (String, String?) -> String = { name, nameRu -> nameRu ?: name }

    //UTC: the app's Joda (android.joda) has no time-zone data off a phone
    private fun day(day: Int) = DateTime(2020, 1, day, 0, 0, DateTimeZone.UTC)

    private fun rate(id: Long, name: String, nameRu: String?, score: Int = 0, updated: DateTime? = null): Rate {
        val anime = Anime(id, name, nameRu, Image(null, null, null, null), "/animes/$id", AnimeType.TV, null,
                Status.RELEASED, 12, 12, null, null)
        return Rate(id, score, RateStatus.WATCHING, null, null, 0, null, null, null, null, updated, anime, null)
    }

    private val naruto = rate(1, "Naruto", "Наруто", score = 7, updated = day(3))
    private val bleach = rate(2, "Bleach", "Блич", score = 9, updated = day(1))
    private val monster = rate(3, "Monster", null, score = 8, updated = day(2))
    private val all = listOf(naruto, bleach, monster)

    private fun order(sort: RateSort, descending: Boolean = false, query: String? = null,
                      titleName: (String, String?) -> String = english) =
            sortAndSearchRates(all, sort, descending, query, titleName).map { it.id }

    @Test
    fun `the search finds either name, in any case`() {
        assertEquals(listOf(2L), order(RateSort.Id, query = "блич"))
        assertEquals(listOf(1L), order(RateSort.Id, query = "NARU"))
    }

    @Test
    fun `a blank search keeps every rate`() {
        assertEquals(listOf(1L, 2L, 3L), order(RateSort.Id, query = " "))
    }

    @Test
    fun `the name sort uses the name the setting shows`() {
        assertEquals(listOf(2L, 3L, 1L), order(RateSort.Name, titleName = english))
        //Monster has no russian name, so it sorts by the one it shows
        assertEquals(listOf(3L, 2L, 1L), order(RateSort.Name, titleName = russian))
    }

    @Test
    fun `descending reverses the order`() {
        assertEquals(listOf(2L, 3L, 1L), order(RateSort.Score, descending = true))
        assertEquals(listOf(1L, 3L, 2L), order(RateSort.Score))
    }

    @Test
    fun `the update date sort orders by when the rate changed`() {
        assertEquals(listOf(2L, 3L, 1L), order(RateSort.DateUpdated))
    }
}

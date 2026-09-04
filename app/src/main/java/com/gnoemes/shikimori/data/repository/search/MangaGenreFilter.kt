package com.gnoemes.shikimori.data.repository.search

/**
 * Works around shikimori having migrated its **anime** catalog filter to v2 genres and **not its
 * manga one**.
 *
 * All 80 anime genres can be filtered by. Of the 81 manga genres, only the 40 whose ids date back
 * to v1 work; the 41 added by v2 return **no results at all**, on graphql and on rest alike, even
 * though a manga happily reports them as its own genres (Monster carries `Удостоено наград`=126,
 * and filtering by 126 finds nothing). Measured 2026-09-02.
 *
 * Two things are done about it:
 *
 * - six of the dead genres existed in v1 under a different manga id, and **that id still filters**.
 *   [mapArgument] swaps them in. This is safe precisely because those v1 ids are *absent* from the
 *   v2 vocabulary, so nothing can collide - unlike the five anime ids that were reused for
 *   different genres, see `GenreV2`;
 * - the remaining 35, all of them themes, are [isFilterable] `false` so the filter screen can leave
 *   them out rather than offer a search that finds nothing.
 *
 * Details chips are deliberately **not** filtered by this: they describe the title truthfully, and
 * a tap that finds nothing is better than a genre silently missing from the page.
 *
 * ⚠️ **This is a snapshot, and it is meant to shrink.** If shikimori ever indexes the manga filter
 * for v2, these lists become wrong in the direction of hiding genres that work. Re-run
 * `filterable_sweep.py` (kept with the genres v2 notes - it probes every id through its own root
 * query, ten per request) before each release that touches genres, and delete whatever now returns
 * results. When [UNFILTERABLE] empties out, this whole file goes with it.
 */
object MangaGenreFilter {

    /**
     * v2 manga genre id -> the v1 manga id that still filters for it.
     *
     * All five *genres* among the dead ids are here; only themes could not be rescued.
     */
    private val V1_FALLBACK = mapOf(
            153L to 81L,    //Триллер
            165L to 55L,    //Сёнен-ай
            170L to 73L,    //Сёдзё-ай
            181L to 542L,   //Работа
            601L to 540L,   //Эротика
            602L to 59L     //Хентай
    )

    /**
     * Manga genres shikimori cannot filter by at all, and that have no v1 id to fall back on.
     * All 35 are themes:
     *
     * 126 Удостоено наград, 127 Жестокость, 152 Шоу-бизнес, 154 Иясикэй, 156 Спортивные
     * единоборства, 157 Командный спорт, 158 Любовный многоугольник, 159 Взрослые персонажи,
     * 160 Махо-сёдзё, 161 Реинкарнация, 162 Магическая смена пола, 164 Реверс-гарем,
     * 166 Изобразительное искусство, 167 Исполнительское искусство, 168 Гэг-юмор, 169 Забота о
     * детях, 171 Организованная преступность, 172 Исэкай, 173 CGDCT, 174 Путешествие во времени,
     * 175 Видеоигры, 176 Выживание, 177 Культура отаку, 179 Хулиганы, 180 Антропоморфизм,
     * 183 Питомцы, 184 Игра с высокими ставками, 185 Медицина, 186 Мемуары, 187 Образовательное,
     * 188 Романтический подтекст, 190 Идолы (Жен.), 191 Идолы (Муж.), 192 Злодейка,
     * 199 Городское фэнтези
     */
    private val UNFILTERABLE = setOf(
            126L, 127L, 152L, 154L, 156L, 157L, 158L, 159L, 160L, 161L, 162L, 164L, 166L, 167L,
            168L, 169L, 171L, 172L, 173L, 174L, 175L, 176L, 177L, 179L, 180L, 183L, 184L, 185L,
            186L, 187L, 188L, 190L, 191L, 192L, 199L
    )

    /**
     * Whether a manga search by this genre can return anything. Anime is unaffected - every anime
     * genre filters - so this is only ever asked about manga and ranobe.
     */
    fun isFilterable(id: Long): Boolean = id !in UNFILTERABLE

    /**
     * Rewrites the `genre` argument of a manga query, keeping the `!` exclusion prefix and the
     * comma separated shape: `"153,!168"` becomes `"81,!168"`.
     *
     * Ids with no fallback are passed through untouched. That is what makes a details chip for a
     * dead genre a search with no results rather than an error, and it is deliberate.
     */
    fun mapArgument(value: String): String =
            value.split(",").joinToString(",") { token ->
                val negated = token.startsWith("!")
                val raw = if (negated) token.substring(1) else token
                val id = raw.toLongOrNull() ?: return@joinToString token
                val mapped = V1_FALLBACK[id] ?: id

                if (negated) "!$mapped" else mapped.toString()
            }
}

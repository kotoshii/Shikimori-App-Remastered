package com.gnoemes.shikimori.domain.search

import com.gnoemes.shikimori.entity.anime.domain.Anime
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.manga.domain.Manga
import com.gnoemes.shikimori.entity.roles.domain.Character
import com.gnoemes.shikimori.entity.roles.domain.Person
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Single

interface SearchInteractor {

    fun loadCharacterListWithFilters(filters: HashMap<String, MutableList<FilterItem>>?): Single<List<Character>>

    fun loadPersonListWithFilters(filters: HashMap<String, MutableList<FilterItem>>?): Single<List<Person>>

    //a page of the catalog is the most the api sends: fewer requests while scrolling (P10-T3, the user's call)
    fun loadAnimeListWithFilters(filters: HashMap<String, MutableList<FilterItem>>?, page: Int, limit: Int = Constants.MAX_ANIME_LIMIT): Single<Page<Anime>>

    /** [type] MANGA or RANOBE. */
    fun loadMangaListWithFilters(filters: HashMap<String, MutableList<FilterItem>>?, type: Type, page: Int, limit: Int = Constants.MAX_ANIME_LIMIT): Single<Page<Manga>>
}
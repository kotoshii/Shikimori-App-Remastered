package com.gnoemes.shikimori.presentation.presenter.search.converter

import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.search.domain.CatalogSettings
import com.gnoemes.shikimori.entity.search.presentation.SearchItem

interface SearchViewModelConverter {

    /**
     * The catalog's cards for [items] as loaded (titles, characters or people), with the parts [settings]
     * shows; [statuses] are the user's, by title id, for the type shown.
     */
    fun convert(items: List<Any>, settings: CatalogSettings, statuses: Map<Long, RateStatus>): List<SearchItem>
}

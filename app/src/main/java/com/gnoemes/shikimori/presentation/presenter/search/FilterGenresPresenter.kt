package com.gnoemes.shikimori.presentation.presenter.search

import com.arellomobile.mvp.InjectViewState
import com.gnoemes.shikimori.domain.search.filter.FilterInteractor
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.presentation.presenter.base.BaseFilterPresenter
import com.gnoemes.shikimori.presentation.presenter.search.converter.FilterViewModelConverter
import com.gnoemes.shikimori.presentation.view.search.filter.genres.FilterGenresView
import javax.inject.Inject

/**
 * The chip panel: «Жанры», in sections, and since P10-T4 «Лицензия» too, as one block ([filterType]).
 */
@InjectViewState
class FilterGenresPresenter @Inject constructor(
        interactor: FilterInteractor,
        private val converter: FilterViewModelConverter
) : BaseFilterPresenter<FilterGenresView>(interactor) {

    /** The row it was opened for. */
    var filterType = FilterType.GENRE

    override fun loadData() =
            panelCategory(filterType)
                    .map { if (filterType == FilterType.GENRE) converter.convertGenres(it, appliedFilters) else converter.convertSection(it, appliedFilters) }
                    .subscribe(this::setData, this::processErrors)
                    .addToDisposables()

    private fun setData(it : List<Any>) {
        viewState.showData(it)
    }

}
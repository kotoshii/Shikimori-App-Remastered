package com.gnoemes.shikimori.presentation.presenter.base

import com.gnoemes.shikimori.domain.search.filter.FilterInteractor
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFilterView
import io.reactivex.Single

abstract class BaseFilterPresenter<View : BaseFilterView>(
        private val filterInteractor: FilterInteractor
) : BaseNetworkPresenter<View>() {

    lateinit var type: Type
    lateinit var appliedFilters: HashMap<String, MutableList<FilterItem>>

    override fun initData() {
        onFiltersChanged()
    }

    abstract fun loadData()

    /** The filter categories of this [type]: anime, manga or ranobe each have their own. */
    protected fun loadCategories(): Single<List<FilterCategory>> = when (type) {
        Type.MANGA -> filterInteractor.getMangaFilters()
        Type.RANOBE -> filterInteractor.getRanobeFilters()
        else -> filterInteractor.getAnimeFilters()
    }

    protected open fun onFiltersChanged() {
        viewState.setResetEnabled(appliedFilters.size > 0)
        loadData()
    }

    open fun onResetClicked() {
        appliedFilters.clear()
        onFiltersChanged()
    }

    open fun onAcceptClicked() {
        viewState.onFiltersAccepted(appliedFilters)
    }

    //a long press excludes a chip, and on an excluded one makes it a normal pick again - it did nothing
    open fun onFilterInverted(type: FilterType, item: FilterViewModel) {
        when (item.state) {
            FilterViewModel.STATE.INVERTED -> includeAgain(type.value, FilterItem(type.value, item.value, item.text))
            else -> invertOrAddNew(type.value, FilterItem(type.value, item.value, item.text))
        }
        onFiltersChanged()
    }

    open fun onFilterSelected(type: FilterType, item: FilterViewModel) {
        when (item.state) {
            FilterViewModel.STATE.DEFAULT -> addToSelected(type.value, FilterItem(type.value, item.value, item.text))
            else -> removeFromSelected(type.value, FilterItem(type.value, item.value, item.text))
        }
        onFiltersChanged()
    }

    protected open fun getAppliedFilter(key: String, value: String?): FilterItem? {
        return appliedFilters[key]?.find { it.value == value }
    }

    protected open fun invertOrAddNew(key: String, value: FilterItem) {
        val item = getAppliedFilter(key, value.value)
        item?.let { removeFromSelected(key, it); addInverted(key, it) } ?: addInverted(key, value)
    }

    private fun includeAgain(key: String, inverted: FilterItem) {
        val item = getAppliedFilter(key, inverted.value) ?: inverted
        removeFromSelected(key, item)
        addToSelected(key, item.copy(value = item.value?.invertValue()))
    }

    protected open fun addInverted(key: String, item: FilterItem) {
        val newItem =
                if (!item.value?.contains("!")!!) item.copy(value = "!${item.value}")
                else item
        addToSelected(key, newItem)
    }

    //added when missing. A chip already there used to replace the whole list with itself: typing a
    //season that was already picked dropped every other season (BUGS #59 N3)
    protected open fun addToSelected(key: String, value: FilterItem) {
        val list = appliedFilters[key]
        if (list == null) appliedFilters[key] = mutableListOf(value)
        else if (!list.contains(value)) list.add(value)
    }

    //the key goes with its last item - only when that item is the one asked for: a one-item list was
    //dropped whatever item was asked (BUGS #69 B14). The lists are this panel's own copies
    protected open fun removeFromSelected(key: String, value: FilterItem) {
        val list = appliedFilters[key] ?: return
        list.remove(value)
        if (list.isEmpty()) appliedFilters.remove(key)
    }

    protected fun String.invertValue(): String = if (this.contains("!")) this.replace("!", "") else "!${this}"
}
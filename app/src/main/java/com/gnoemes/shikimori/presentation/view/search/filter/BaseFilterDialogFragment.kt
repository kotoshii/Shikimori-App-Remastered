package com.gnoemes.shikimori.presentation.view.search.filter

import android.content.Context
import android.graphics.Point
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.presentation.presenter.base.BaseFilterPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetInjectionDialogFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFilterView
import com.gnoemes.shikimori.utils.dimenAttr
import com.gnoemes.shikimori.utils.withArgs

/**
 * The catalog's filter sheet and its genre and season panels: what they are opened with, how their
 * presenter starts, and handing the chosen filters back. Each keeps its own layout and adapters.
 */
abstract class BaseFilterDialogFragment<P : BaseFilterPresenter<V>, V : BaseFilterView>
    : BaseBottomSheetInjectionDialogFragment<P, V>(), BaseFilterView {

    companion object {
        private const val FILTERS_KEY = "FILTERS_KEY"
        private const val TYPE_KEY = "TYPE_KEY"

        /** [fragment] opened for [type] with the filters applied so far. */
        fun <F : BaseFilterDialogFragment<*, *>> withFilters(fragment: F, type: Type, filters: HashMap<String, MutableList<FilterItem>>?): F =
                fragment.withArgs {
                    putSerializable(TYPE_KEY, type)
                    putSerializable(FILTERS_KEY, filters)
                }
    }

    /** The genre and season panels open almost full height; the sheet itself does not. */
    protected open val isTall: Boolean = false

    /** For the `@ProvidePresenter` of each: the presenter starts with the arguments, on a copy of the filters. */
    @Suppress("UNCHECKED_CAST")
    protected fun P.withArguments(): P = apply {
        type = arguments?.getSerializable(TYPE_KEY) as? Type ?: Type.ANIME
        appliedFilters = HashMap(arguments?.getSerializable(FILTERS_KEY) as HashMap<String, MutableList<FilterItem>>)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (isTall) peekHeight = Point().let { activity?.windowManager?.defaultDisplay?.getSize(it);it }.x - context.dimenAttr(android.R.attr.actionBarSize)
    }

    override fun onFiltersAccepted(appliedFilters: HashMap<String, MutableList<FilterItem>>) {
        (targetFragment as? FilterCallback)?.onFiltersSelected(tag, appliedFilters)
        onBackPressed()
    }
}

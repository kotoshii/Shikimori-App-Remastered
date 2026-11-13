package com.gnoemes.shikimori.presentation.view.search.filter

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentFilterBinding
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.presentation.presenter.search.FilterPresenter
import com.gnoemes.shikimori.presentation.view.common.fragment.ChoiceDialog
import com.gnoemes.shikimori.presentation.view.search.filter.BaseFilterDialogFragment
import com.gnoemes.shikimori.presentation.view.search.filter.adapter.FilterAdapter
import com.gnoemes.shikimori.presentation.view.search.filter.genres.FilterGenresFragment
import com.gnoemes.shikimori.presentation.view.search.filter.seasons.FilterSeasonsFragment
import com.gnoemes.shikimori.utils.*

class FilterFragment : BaseFilterDialogFragment<FilterPresenter, FilterView>(), FilterView, FilterCallback, ChoiceDialog.ChoiceCallback {

    @InjectPresenter
    lateinit var filterPresenter: FilterPresenter

    @ProvidePresenter
    fun providePresenter(): FilterPresenter = presenterProvider.get().withArguments()
            .apply {
                handedSorts = arguments?.getParcelableArrayList(SORTS_KEY)
                searching = arguments?.getBoolean(SEARCHING_KEY) ?: false
            }

    companion object {
        private const val HINT_KEY = "HINT_KEY"
        private const val GENRES_TAG = "genresFilterDialog"
        private const val SEASONS_TAG = "seasonsFilterDialog"
        private const val SORTS_KEY = "SORTS_KEY"
        private const val SEARCHING_KEY = "SEARCHING_KEY"
        private const val SORT_REQUEST = "SORT_REQUEST"

        /**
         * The sheet; with the [categories] and [sorts] when the catalog has them, so it opens filled.
         * [searching]: «Применить» will search a text, so the sort does not apply.
         */
        fun newInstance(type: Type, filters: HashMap<String, MutableList<FilterItem>>?,
                        categories: List<FilterCategory>?, sorts: List<FilterItem>?, searching: Boolean) =
                withFilters(FilterFragment(), type, filters, categories).apply {
                    arguments?.putParcelableArrayList(SORTS_KEY, sorts?.let { ArrayList(it) })
                    arguments?.putBoolean(SEARCHING_KEY, searching)
                }
    }

    private val binding by viewBinding(FragmentFilterBinding::bind)

    private val adapter by lazy { FilterAdapter(presenter::onFilterAction, presenter::onFilterSelected, presenter::onFilterInverted) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (context!!.getDefaultSharedPreferences().getBoolean(HINT_KEY, false)) binding.hintContainer.gone()
        else {
            binding.hintContainer.onClick {
                putSetting(HINT_KEY, true)
                TransitionManager.beginDelayedTransition(binding.appBarLayout, ChangeBounds())
                binding.hintContainer.gone()
            }
        }

        with(binding.toolbar) {
            setTitle(R.string.filters)
            addBackButton(R.drawable.ic_close) { onBackPressed() }
        }

        binding.resetBtn.onClick { presenter.onResetClicked() }
        binding.filterBottom.acceptBtn.onClick { presenter.onAcceptClicked() }
        binding.filterBottom.sortBtn.onClick { presenter.onSortClicked() }

        with(binding.list) {
            adapter = this@FilterFragment.adapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            itemAnimator = null
        }
    }

    override fun onFiltersSelected(tag: String?, appliedFilters: HashMap<String, MutableList<FilterItem>>) {
        val key = when (tag) {
            GENRES_TAG -> FilterType.GENRE.value
            SEASONS_TAG -> FilterType.SEASON.value
            else -> null
        }

        presenter.onNestedFilterCallback(key, appliedFilters)
    }

    override fun onChoiceClicked(requestKey: String, key: String) {
        if (requestKey == SORT_REQUEST) presenter.onSortPicked(key)
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getDialogLayout(): Int = R.layout.fragment_filter

    override val presenter: FilterPresenter
        get() = filterPresenter

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showData(items: List<Any>) {
        adapter.bindItems(items)
    }

    override fun showGenresDialog(type: Type, filters: HashMap<String, MutableList<FilterItem>>, categories: List<FilterCategory>?) {
        val fragment = fragmentManager?.findFragmentByTag(GENRES_TAG)
        if (fragment == null) {
            val filter = FilterGenresFragment.newInstance(type, filters, categories)
            filter.setTargetFragment(this, 43)
            postViewAction { filter.show(fragmentManager!!, GENRES_TAG) }
        }
    }

    override fun showSeasonsDialog(type: Type, filters: HashMap<String, MutableList<FilterItem>>, categories: List<FilterCategory>?) {
        val fragment = fragmentManager?.findFragmentByTag(SEASONS_TAG)
        if (fragment == null) {
            val filter = FilterSeasonsFragment.newInstance(type, filters, categories)
            filter.setTargetFragment(this, 44)
            postViewAction { filter.show(fragmentManager!!, SEASONS_TAG) }
        }
    }

    override fun setResetEnabled(show: Boolean) {
        binding.resetBtn.isEnabled = show
    }

    //the sort sheet «Списки» uses: titled, the current sort marked (BUGS #16b)
    override fun showSortFilters(items: List<FilterItem>, chosen: String) {
        ChoiceDialog.newInstance(SORT_REQUEST, getString(R.string.sort),
                items.map { ChoiceDialog.Choice(it.value!!, it.localizedText!!, it.value == chosen) })
                .show(childFragmentManager, "SortsTag")
    }

    override fun setSortFilterText(text: String) {
        binding.filterBottom.sortBtn.text = text
    }

    override fun showSortBySearch() {
        binding.filterBottom.sortBtn.setText(R.string.catalog_sort_by_search)
        binding.filterBottom.sortBtn.setUsable(false)
    }
}
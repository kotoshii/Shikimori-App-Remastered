package com.gnoemes.shikimori.presentation.view.search.filter.genres

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentFilterGenresBinding
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.presentation.presenter.search.FilterGenresPresenter
import com.gnoemes.shikimori.presentation.view.search.filter.BaseFilterDialogFragment
import com.gnoemes.shikimori.presentation.view.search.filter.genres.adapter.FilterGenreAdapter
import com.gnoemes.shikimori.utils.*

class FilterGenresFragment : BaseFilterDialogFragment<FilterGenresPresenter, FilterGenresView>(), FilterGenresView {

    @InjectPresenter
    lateinit var filterPresenter: FilterGenresPresenter

    @ProvidePresenter
    fun providePresenter(): FilterGenresPresenter = presenterProvider.get().withArguments()
            .apply { filterType = this@FilterGenresFragment.filterType }

    companion object {
        private const val FILTER_TYPE_KEY = "FILTER_TYPE_KEY"

        /** The chip panel for [filterType]: «Жанры», or «Лицензия» (P10-T4). */
        fun newInstance(type: Type, filterType: FilterType, filters: HashMap<String, MutableList<FilterItem>>?, categories: List<FilterCategory>?) =
                withFilters(FilterGenresFragment(), type, filters, categories).apply {
                    arguments?.putSerializable(FILTER_TYPE_KEY, filterType)
                }
    }

    private val filterType: FilterType
        get() = arguments?.getSerializable(FILTER_TYPE_KEY) as? FilterType ?: FilterType.GENRE

    private val binding by viewBinding(FragmentFilterGenresBinding::bind)

    private val adapter by lazy { FilterGenreAdapter(presenter::onFilterInverted, presenter::onFilterSelected) }

    override val isTall: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.appBarLayout.toolbar) {
            setTitle(if (filterType == FilterType.GENRE) R.string.filters_genres else R.string.filter_licensor)
            addBackButton(R.drawable.ic_close) { onBackPressed() }
        }

        with(binding.recyclerView) {
            adapter = this@FilterGenresFragment.adapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        binding.appBarLayout.clearBtn.onClick { presenter.onResetClicked() }
        binding.appBarLayout.acceptBtn.onClick { presenter.onAcceptClicked() }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override val presenter: FilterGenresPresenter
        get() = filterPresenter

    override fun getDialogLayout(): Int = R.layout.fragment_filter_genres

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showData(items: List<Any>) {
        adapter.bindItems(items)
    }

    override fun setResetEnabled(show: Boolean) = binding.appBarLayout.clearBtn.visibleIf { show }

}
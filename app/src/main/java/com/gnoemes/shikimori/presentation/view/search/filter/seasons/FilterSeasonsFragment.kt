package com.gnoemes.shikimori.presentation.view.search.filter.seasons

import android.os.Bundle
import android.text.Html
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentFilterSeasonsBinding
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.presentation.presenter.search.FilterSeasonsPresenter
import com.gnoemes.shikimori.presentation.view.search.filter.BaseFilterDialogFragment
import com.gnoemes.shikimori.presentation.view.search.filter.adapter.FilterChipAdapter
import com.gnoemes.shikimori.presentation.view.search.filter.seasons.adapter.FilterSeasonsAdapter
import com.gnoemes.shikimori.utils.*
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager

class FilterSeasonsFragment : BaseFilterDialogFragment<FilterSeasonsPresenter, FilterSeasonsView>(), FilterSeasonsView {

    @InjectPresenter
    lateinit var filterPresenter: FilterSeasonsPresenter

    @ProvidePresenter
    fun providePresenter(): FilterSeasonsPresenter = presenterProvider.get().withArguments()

    companion object {
        fun newInstance(type: Type, filters: HashMap<String, MutableList<FilterItem>>?, categories: List<FilterCategory>?) =
                withFilters(FilterSeasonsFragment(), type, filters, categories)
    }

    private val binding by viewBinding(FragmentFilterSeasonsBinding::bind)

    private val simpleAdapter by lazy { FilterChipAdapter(FilterType.SEASON, presenter::onFilterInverted, presenter::onFilterSelected) }
    private val customAdapter by lazy {
        FilterSeasonsAdapter(presenter::onNewCustomFilter, presenter::onRemoveCustomFilter) { presenter.onTagInverted(FilterType.SEASON, it) }
    }

    override val isTall: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.appBarLayout.toolbar) {
            setTitle(R.string.filter_seasons)
            addBackButton(R.drawable.ic_close) { onBackPressed() }
        }

        with(binding.recyclerView) {
            adapter = simpleAdapter
            layoutManager = FlexboxLayoutManager(context)
            itemAnimator = null
        }

        with(binding.seasonsCustom.customInputRecyclerView) {
            adapter = customAdapter
            layoutManager = FlexboxLayoutManager(context).apply { flexWrap = FlexWrap.WRAP; flexDirection = FlexDirection.ROW }
            itemAnimator = null
        }

        binding.appBarLayout.clearBtn.onClick { presenter.onResetClicked() }
        binding.appBarLayout.acceptBtn.onClick { presenter.onAcceptClicked() }
        binding.seasonsCustom.helpBtn.onClick {
            context!!.materialDialog().show {
                title(R.string.filter_custom)
                message(text = Html.fromHtml(getString(R.string.filter_custom_seasons_hint)))
                positiveButton(R.string.common_understand)
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override val presenter: FilterSeasonsPresenter
        get() = filterPresenter

    override fun getDialogLayout(): Int = R.layout.fragment_filter_seasons

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showSimpleData(items: List<FilterViewModel>) {
        simpleAdapter.bind(items)
    }

    override fun showCustomData(items: List<Any>) {
        customAdapter.bindItems(items)
    }

    override fun setResetEnabled(show: Boolean) = binding.appBarLayout.clearBtn.visibleIf { show }


    override fun onBackPressed() {
        dismiss()
    }

    override fun showData(items: List<Any>) = Unit
}
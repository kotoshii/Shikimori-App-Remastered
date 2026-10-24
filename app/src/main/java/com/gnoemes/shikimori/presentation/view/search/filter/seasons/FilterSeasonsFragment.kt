package com.gnoemes.shikimori.presentation.view.search.filter.seasons

import android.os.Bundle
import android.text.Html
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.presentation.presenter.search.FilterSeasonsPresenter
import com.gnoemes.shikimori.presentation.view.search.filter.BaseFilterDialogFragment
import com.gnoemes.shikimori.presentation.view.search.filter.adapter.FilterChipAdapter
import com.gnoemes.shikimori.presentation.view.search.filter.seasons.adapter.FilterSeasonsAdapter
import com.gnoemes.shikimori.utils.*
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.android.synthetic.main.fragment_filter_seasons.*
import kotlinx.android.synthetic.main.layout_filter_nested_toolbar.*
import kotlinx.android.synthetic.main.layout_filter_seasons_custom.*

class FilterSeasonsFragment : BaseFilterDialogFragment<FilterSeasonsPresenter, FilterSeasonsView>(), FilterSeasonsView {

    @InjectPresenter
    lateinit var filterPresenter: FilterSeasonsPresenter

    @ProvidePresenter
    fun providePresenter(): FilterSeasonsPresenter = presenterProvider.get().withArguments()

    companion object {
        fun newInstance(type: Type, filters: HashMap<String, MutableList<FilterItem>>?) = withFilters(FilterSeasonsFragment(), type, filters)
    }

    private val simpleAdapter by lazy { FilterChipAdapter(FilterType.SEASON, presenter::onFilterInverted, presenter::onFilterSelected) }
    private val customAdapter by lazy { FilterSeasonsAdapter(presenter::onNewCustomFilter, presenter::onRemoveCustomFilter) }

    override val isTall: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(toolbar) {
            setTitle(R.string.filter_seasons)
            addBackButton(R.drawable.ic_close) { onBackPressed() }
        }

        with(recyclerView) {
            adapter = simpleAdapter
            layoutManager = FlexboxLayoutManager(context)
            itemAnimator = null
        }

        with(customInputRecyclerView) {
            adapter = customAdapter
            layoutManager = FlexboxLayoutManager(context).apply { flexWrap = FlexWrap.WRAP; flexDirection = FlexDirection.ROW }
            itemAnimator = null
        }

        clearBtn.onClick { presenter.onResetClicked() }
        acceptBtn.onClick { presenter.onAcceptClicked() }
        helpBtn.onClick {
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

    override fun setResetEnabled(show: Boolean) = clearBtn.visibleIf { show }


    override fun onBackPressed() {
        dismiss()
    }

    override fun showData(items: List<Any>) = Unit
}
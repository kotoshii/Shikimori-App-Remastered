package com.gnoemes.shikimori.presentation.view.search.filter.companies

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentFilterCompaniesBinding
import com.gnoemes.shikimori.databinding.LayoutDefaultPlaceholdersBinding
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.domain.FilterType
import com.gnoemes.shikimori.entity.search.presentation.FilterEntryViewModel
import com.gnoemes.shikimori.entity.search.presentation.FilterViewModel
import com.gnoemes.shikimori.presentation.presenter.search.FilterCompaniesPresenter
import com.gnoemes.shikimori.presentation.view.search.filter.BaseFilterDialogFragment
import com.gnoemes.shikimori.presentation.view.search.filter.companies.adapter.FilterCompanyAdapter
import com.gnoemes.shikimori.presentation.view.search.filter.companies.adapter.FilterTagAdapter
import com.gnoemes.shikimori.utils.*
import com.google.android.flexbox.FlexboxLayoutManager

/** The «Студии» / «Издатели» panel (FilterCompaniesPresenter), full height: the list is long. */
class FilterCompaniesFragment : BaseFilterDialogFragment<FilterCompaniesPresenter, FilterCompaniesView>(), FilterCompaniesView {

    @InjectPresenter
    lateinit var filterPresenter: FilterCompaniesPresenter

    @ProvidePresenter
    fun providePresenter(): FilterCompaniesPresenter = presenterProvider.get().withArguments()

    companion object {
        fun newInstance(type: Type, filters: HashMap<String, MutableList<FilterItem>>?) =
                withFilters(FilterCompaniesFragment(), type, filters)
    }

    private val binding by viewBinding(FragmentFilterCompaniesBinding::bind)

    //layout_default_placeholders is a <merge>: its views sit straight in the panel's layout
    private val placeholders by viewBinding(LayoutDefaultPlaceholdersBinding::bind)

    override val networkErrorView get() = placeholders.networkErrorView

    private val filterType by lazy { FilterType.companyFor(catalogType) }

    private val chosenAdapter by lazy { FilterTagAdapter(presenter::onTagRemoved) { presenter.onTagInverted(filterType, it) } }

    private val listAdapter by lazy { FilterCompanyAdapter(filterType, presenter::onFilterSelected, presenter::onFilterInverted) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val studios = filterType == FilterType.STUDIO

        with(binding.appBarLayout.toolbar) {
            setTitle(if (studios) R.string.filter_studios else R.string.filter_publishers)
            addBackButton(R.drawable.ic_close) { onBackPressed() }
        }
        binding.appBarLayout.clearBtn.onClick { presenter.onResetClicked() }
        binding.appBarLayout.acceptBtn.onClick { presenter.onAcceptClicked() }

        binding.hint.setText(if (studios) R.string.filter_studios_hint else R.string.filter_publishers_hint)

        with(binding.searchView) {
            //the app's search field: its ✕ in the theme's text colour (it was black on the dark themes). It
            //takes away the margin before the 🔍, which the field's own padding gives back (layout)
            applyAppStyle(startPaddingDp = 8)
            queryHint = getString(if (studios) R.string.filter_studio_search_hint else R.string.filter_publisher_search_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    hideSoftInput()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    presenter.onQueryChanged(newText.orEmpty())
                    return true
                }
            })
        }

        with(binding.chosenList) {
            adapter = chosenAdapter
            layoutManager = FlexboxLayoutManager(context)
            //as every chip and tag list in the filter sheets: with the animator a new tag blinked once
            itemAnimator = null
        }

        with(binding.recyclerView) {
            adapter = listAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
            setHasFixedSize(true)
        }

        placeholders.emptyContentView.setText(R.string.common_empty)
        //the panel has «Повторить», not a pull to refresh
        placeholders.networkErrorView.setText(R.string.common_error_message_without_pull)
        placeholders.networkErrorView.showButton()
        placeholders.networkErrorView.callback = { presenter.onRetryClicked() }
        placeholders.emptyContentView.gone()
        placeholders.networkErrorView.gone()
    }

    //✕, «✓» and the system back: the keyboard goes with the panel
    override fun onBackPressed() {
        hideSoftInput()
        super.onBackPressed()
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override val presenter: FilterCompaniesPresenter
        get() = filterPresenter

    override fun getDialogLayout(): Int = R.layout.fragment_filter_companies

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showCompanies(chosen: List<FilterEntryViewModel>, rows: List<FilterViewModel>?) {
        chosenAdapter.bindItems(chosen)
        binding.chosenList.visibleIf { chosen.isNotEmpty() }
        listAdapter.bind(rows.orEmpty())
        placeholders.emptyContentView.visibleIf { rows != null && rows.isEmpty() }
    }

    override fun showListLoading(show: Boolean) = binding.progressBar.visibleIf { show }

    //the panel draws everything through showCompanies
    override fun showData(items: List<Any>) = Unit

    override fun setResetEnabled(show: Boolean) = binding.appBarLayout.clearBtn.visibleIf { show }
}

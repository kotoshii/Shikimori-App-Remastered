package com.gnoemes.shikimori.presentation.view.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentSearchBinding
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.entity.search.presentation.SearchItem
import com.gnoemes.shikimori.entity.search.presentation.SearchNavigationData
import com.gnoemes.shikimori.presentation.presenter.search.SearchPresenter
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter
import com.gnoemes.shikimori.presentation.view.base.fragment.BasePaginationFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.base.fragment.TabRootFragment
import com.gnoemes.shikimori.presentation.view.common.fragment.ChoiceDialog
import com.gnoemes.shikimori.presentation.view.search.adapter.SearchAdapter
import com.gnoemes.shikimori.presentation.view.search.filter.FilterCallback
import com.gnoemes.shikimori.presentation.view.search.filter.FilterFragment
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.widgets.GridItemDecorator
import com.gnoemes.shikimori.utils.widgets.OverlapHeaderScrollingBehavior
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class SearchFragment : BasePaginationFragment<SearchItem, SearchPresenter, SearchView>(), SearchView, FilterCallback, ChoiceDialog.ChoiceCallback, HasSupportFragmentInjector, TabRootFragment {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var searchPresenter: SearchPresenter

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    @ProvidePresenter
    fun providePresenter(): SearchPresenter {
        searchPresenter = presenterProvider.get()

        parentFragment.ifNotNull {
            searchPresenter.localRouter = (parentFragment as RouterProvider).localRouter
        }

        arguments.ifNotNull {
            val data = it.getParcelable<SearchNavigationData>(AppExtras.ARGUMENT_SEARCH_DATA)
            data?.let {
                searchPresenter.searchPayload = data.payload
                searchPresenter.type = data.type
            }
        }

        return searchPresenter
    }

    companion object {
        private const val TYPE_REQUEST = "TYPE_REQUEST"
        private const val TYPE_SHEET_TAG = "typeSheet"

        fun newInstance(data: SearchNavigationData?) = SearchFragment().withArgs { putParcelable(AppExtras.ARGUMENT_SEARCH_DATA, data) }
    }

    private val searchAdapter by lazy { SearchAdapter(imageLoader, getPresenter()::onContentClicked).apply { if (!hasObservers()) setHasStableIds(true) } }

    override val adapter: BasePaginationAdapter
        get() = searchAdapter

    //its own layout, the floating bar over the list, as «Списки» and «Календарь» (P10-T2)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(getFragmentLayout(), container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //what to search, picked in a sheet. A catalog opened with a genre or studio has the back arrow
        //here instead (addBackButton)
        with(binding.toolbarCard.toolbar) {
            title = null
            setNavigationIcon(R.drawable.ic_chevron_down)
            setNavigationContentDescription(R.string.search_type_title)
            setNavigationOnClickListener { getPresenter().onTypeClicked() }
        }

        with(binding.toolbarCard.searchView) {
            setOnQueryTextListener(searchViewQueryListener)
            applyAppStyle()
        }

        with(defaultList.recyclerView) {
            val spanCount = context.calculateColumns(R.dimen.image_search_width)
            adapter = this@SearchFragment.adapter
            layoutManager = GridLayoutManager(context, spanCount).apply {
                //the spinner and «Повторить» take the whole row: in one cell the spinner sat on the
                //left, and «Повторить»'s text did not fit
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int =
                            if (searchAdapter.isEndRowAt(position)) spanCount else 1
                }
            }
            setHasFixedSize(true)
            addItemDecoration(GridItemDecorator(context.dimen(R.dimen.margin_small).toInt()))
            //the first row starts below the bar, as on «Списки», and the rows scroll on under it
            clipToPadding = false
            updatePadding(top = context.dp(64))
            addOnScrollListener(nextPageListener)
        }

        defaultList.refreshLayout.layoutParams = (defaultList.refreshLayout.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            behavior = OverlapHeaderScrollingBehavior()
        }
        //the pull's circle starts behind the bar and stops below it
        defaultList.refreshLayout.setProgressViewOffset(false, context!!.dp(24), context!!.dp(96))

        binding.fab.setOnClickListener { getPresenter().onFilterClicked() }
    }

    //the bar back if it slid away, and the field ready to type in (P10-T2 Q3)
    override fun onTabRootAction() {
        if (view == null) return
        binding.appBarLayout.setExpanded(true)
        val field = binding.toolbarCard.searchView.findViewById<View>(R.id.search_src_text) ?: return
        if (field.requestFocus()) context?.inputMethodManager()?.showSoftInput(field, 0)
    }

    private val searchViewQueryListener = object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            getPresenter().onQuerySearch(query)
            hideSoftInput()
            return true
        }

        //every change: the presenter keeps what the field shows (P10-T2 Q18); an empty or blank field drops
        //the search
        override fun onQueryTextChange(newText: String?): Boolean {
            getPresenter().onQueryTextChanged(newText.orEmpty())
            return true
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): SearchPresenter = searchPresenter

    private val binding by viewBinding(FragmentSearchBinding::bind)

    override fun getFragmentLayout(): Int = R.layout.fragment_search

    ///////////////////////////////////////////////////////////////////////////
    // CALLBACKS
    ///////////////////////////////////////////////////////////////////////////

    override fun onFiltersSelected(tag: String?, appliedFilters: HashMap<String, MutableList<FilterItem>>) {
        getPresenter().onFilterSelected(appliedFilters)
    }

    //a type's key in the sheet is its place in search_types
    override fun onChoiceClicked(requestKey: String, key: String) {
        if (requestKey == TYPE_REQUEST) getPresenter().onTypeChanged(key.toInt())
    }

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showFilter(type: Type, filters: HashMap<String, MutableList<FilterItem>>, categories: List<FilterCategory>?, sorts: List<FilterItem>?) {
        hideSoftInput()
        val tag = "filterDialog"
        val fragment = fragmentManager?.findFragmentByTag(tag)
        if (fragment == null) {
            val filter = FilterFragment.newInstance(type, filters, categories, sorts)
            filter.setTargetFragment(this, 42)
            postViewAction { filter.show(fragmentManager!!, tag) }
        }
    }

    override fun showTypeSheet(currentPos: Int) {
        if (childFragmentManager.findFragmentByTag(TYPE_SHEET_TAG) != null) return
        hideSoftInput()
        val choices = resources.getStringArray(R.array.search_types)
                .mapIndexed { pos, text -> ChoiceDialog.Choice(pos.toString(), text, pos == currentPos) }
        ChoiceDialog.newInstance(TYPE_REQUEST, getString(R.string.search_type_title), choices)
                .show(childFragmentManager, TYPE_SHEET_TAG)
    }

    //the field says what it searches, as on «Списки»
    override fun selectType(newTypePos: Int) {
        binding.toolbarCard.searchView.queryHint = resources.getStringArray(R.array.search_types)[newTypePos]
    }

    //comes again with every change of the field's text: set only when it differs, as setting the field's
    //own text again moves the cursor to the end
    override fun showQuery(query: String) {
        val field = binding.toolbarCard.searchView
        if (field.query.toString() != query) field.setQuery(query, false)
    }

    //a catalog opened with a genre or studio: its type came with it, so the back arrow takes the type
    //picker's place (P10-T2 Q4)
    override fun addBackButton() {
        val toolbar = binding.toolbarCard.toolbar
        toolbar.addBackButton { getPresenter().onBackPressed() }
        toolbar.navigationContentDescription = null
    }

    override fun updateFilterIcon(empty: Boolean) {
        binding.fab.setImageResource(if (empty) R.drawable.ic_filter else R.drawable.ic_filter_edit)
    }

    override fun showFilterButton() = binding.fab.show()
    override fun hideFilterButton() = binding.fab.hide()
    override fun setSimpleEmptyText() {
        screenEmptyView?.setText(R.string.search_need_query)
    }
    override fun setDefaultEmptyText() {
        screenEmptyView?.setText(R.string.search_nothing)
    }
}

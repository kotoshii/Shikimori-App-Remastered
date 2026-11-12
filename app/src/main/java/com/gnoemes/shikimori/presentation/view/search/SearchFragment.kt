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
import com.gnoemes.shikimori.entity.search.domain.CatalogSettings
import com.gnoemes.shikimori.entity.search.presentation.FilterCategory
import com.gnoemes.shikimori.entity.search.presentation.SearchNavigationData
import com.gnoemes.shikimori.presentation.presenter.search.SearchPresenter
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter
import com.gnoemes.shikimori.presentation.view.base.fragment.BasePaginationFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.base.fragment.TabRootFragment
import com.gnoemes.shikimori.presentation.view.common.fragment.ChoiceDialog
import com.gnoemes.shikimori.presentation.view.search.adapter.CatalogCardLook
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
import kotlin.math.max

class SearchFragment : BasePaginationFragment<Any, SearchPresenter, SearchView>(), SearchView, FilterCallback, ChoiceDialog.ChoiceCallback,
        CatalogSettingsDialog.Callback, HasSupportFragmentInjector, TabRootFragment {

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
        private const val SETTINGS_SHEET_TAG = "catalogSettingsSheet"

        //the grid (P10-T3): the column count is chosen for an upright phone this wide, the list's sides
        //match the search bar's, and the cards are this far apart
        private const val PHONE_WIDTH_DP = 360
        private const val SIDE_DP = 16
        private const val GAP_DP = 8

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
            //«Отображение», after the field's ✕ (P10-T3 Q3)
            inflateMenu(R.menu.menu_search)
            setOnMenuItemClickListener {
                if (it.itemId == R.id.item_catalog_settings) getPresenter().onSettingsClicked()
                true
            }
        }

        with(binding.toolbarCard.searchView) {
            setOnQueryTextListener(searchViewQueryListener)
            applyAppStyle()
        }

        with(defaultList.recyclerView) {
            adapter = this@SearchFragment.adapter
            //the count comes with the settings (showSettings)
            val grid = GridLayoutManager(context, CatalogSettings.MIN_COLUMNS)
            //the spinner and «Повторить» take the whole row: in one cell the spinner sat on the
            //left, and «Повторить»'s text did not fit. The count of now: a changed setting changes it
            grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int =
                        if (searchAdapter.isEndRowAt(position)) grid.spanCount else 1
            }
            layoutManager = grid
            setHasFixedSize(true)
            //every card gets half the gap on each side, so all are the same width; the list's own padding
            //makes up the rest of the sides. A wider margin on the end cards alone made them narrower
            //than the middle ones
            val gap = context.dp(GAP_DP)
            val halfGap = gap / 2
            addItemDecoration(GridItemDecorator(gap, halfGap))
            //the first row starts below the bar, as on «Списки», and the rows scroll on under it. 12dp below the
            //bar (with the first row's own half gap), as the rows are apart: it was 8 (the user's device test, 2026-11-12)
            clipToPadding = false
            updatePadding(left = context.dp(SIDE_DP) - halfGap, top = context.dp(68), right = context.dp(SIDE_DP) - halfGap)
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

    override fun onCatalogSettingsChanged(settings: CatalogSettings) = getPresenter().onSettingsChanged(settings)

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

    override fun showSettingsSheet(settings: CatalogSettings, signedIn: Boolean) {
        if (childFragmentManager.findFragmentByTag(SETTINGS_SHEET_TAG) != null) return
        hideSoftInput()
        CatalogSettingsDialog.newInstance(settings, signedIn).show(childFragmentManager, SETTINGS_SHEET_TAG)
    }

    //the chosen count is for an upright phone: there a card is this wide, and every screen fits as many
    //cards of that width as it can, never fewer than chosen. So a phone on its side, a tablet or a wide
    //window keep phone-sized cards and show more in a row (P10-T3)
    override fun showSettings(settings: CatalogSettings) {
        val list = defaultList.recyclerView
        val grid = list.layoutManager as? GridLayoutManager ?: return
        val columns = columnsFor(settings.columns)
        if (grid.spanCount != columns) {
            grid.spanCount = columns
            list.invalidateItemDecorations()
        }
        searchAdapter.setLook(CatalogCardLook.of(cardWidthDp(columns)))
    }

    private fun columnsFor(chosen: Int): Int {
        val leastWidth = (PHONE_WIDTH_DP - 2 * SIDE_DP - (chosen - 1) * GAP_DP) / chosen.toFloat()
        val fits = ((listWidthDp() - 2 * SIDE_DP + GAP_DP) / (leastWidth + GAP_DP)).toInt()
        return max(chosen, fits)
    }

    private fun cardWidthDp(columns: Int): Float = (listWidthDp() - 2 * SIDE_DP - (columns - 1) * GAP_DP) / columns.toFloat()

    //the window's width, which in split screen is not the display's; the list fills it
    private fun listWidthDp(): Int = resources.configuration.screenWidthDp

    override fun scrollToTop() {
        defaultList.recyclerView.scrollToPosition(0)
        binding.appBarLayout.setExpanded(true)
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
    //the empty message and the error view do not scroll, so a bar that had slid away with the list could not come
    //back over them, and the field's text - maybe why nothing was found - was out of sight (P10-T3 Q23)
    //the base fragment's, not a view interface's default
    override fun showEmptyView() {
        super<BasePaginationFragment>.showEmptyView()
        binding.appBarLayout.setExpanded(true)
    }

    override fun showNetworkView() {
        super<BasePaginationFragment>.showNetworkView()
        binding.appBarLayout.setExpanded(true)
    }

    override fun setSimpleEmptyText() {
        screenEmptyView?.setText(R.string.search_need_query)
    }
    override fun setDefaultEmptyText() {
        screenEmptyView?.setText(R.string.search_nothing)
    }
}

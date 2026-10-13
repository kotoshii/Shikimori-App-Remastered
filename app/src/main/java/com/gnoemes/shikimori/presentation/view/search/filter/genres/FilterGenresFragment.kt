package com.gnoemes.shikimori.presentation.view.search.filter.genres

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.FilterItem
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.presentation.presenter.search.FilterGenresPresenter
import com.gnoemes.shikimori.presentation.view.search.filter.BaseFilterDialogFragment
import com.gnoemes.shikimori.presentation.view.search.filter.genres.adapter.FilterGenreAdapter
import com.gnoemes.shikimori.utils.*
import kotlinx.android.synthetic.main.fragment_filter_genres.*
import kotlinx.android.synthetic.main.layout_filter_nested_toolbar.*

class FilterGenresFragment : BaseFilterDialogFragment<FilterGenresPresenter, FilterGenresView>(), FilterGenresView {

    @InjectPresenter
    lateinit var filterPresenter: FilterGenresPresenter

    @ProvidePresenter
    fun providePresenter(): FilterGenresPresenter = presenterProvider.get().withArguments()

    companion object {
        fun newInstance(type: Type, filters: HashMap<String, MutableList<FilterItem>>?) = withFilters(FilterGenresFragment(), type, filters)
    }

    private val adapter by lazy { FilterGenreAdapter(presenter::onFilterInverted, presenter::onFilterSelected) }

    override val isTall: Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getDialogLayout(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(toolbar) {
            setTitle(R.string.filters_genres)
            addBackButton(R.drawable.ic_close) { onBackPressed() }
        }

        with(recyclerView) {
            adapter = this@FilterGenresFragment.adapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = null
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        clearBtn.onClick { presenter.onResetClicked() }
        acceptBtn.onClick { presenter.onAcceptClicked() }
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

    override fun setResetEnabled(show: Boolean) = clearBtn.visibleIf { show }

}
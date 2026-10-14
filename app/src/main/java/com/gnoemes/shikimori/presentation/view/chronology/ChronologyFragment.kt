package com.gnoemes.shikimori.presentation.view.chronology

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.chronology.ChronologyType
import com.gnoemes.shikimori.entity.common.domain.CommonNavigationData
import com.gnoemes.shikimori.presentation.presenter.chronology.ChronologyPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseRateButtonListFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.chronology.adapter.ChronologyAdapter
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import kotlinx.android.synthetic.main.fragment_chronology.*
import kotlinx.android.synthetic.main.layout_default_list.*
import kotlinx.android.synthetic.main.layout_default_placeholders.*
import kotlinx.android.synthetic.main.layout_toolbar.*
import javax.inject.Inject

class ChronologyFragment : BaseRateButtonListFragment<ChronologyPresenter, ChronologyView>(), ChronologyView, ChronologyTypeDialog.Callback {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var chronologyPresenter: ChronologyPresenter

    @ProvidePresenter
    fun provide() = presenterProvider.get().apply {
        localRouter = (parentFragment as RouterProvider).localRouter
        data = arguments?.getParcelable(DATA_KEY)!!
    }

    companion object {
        fun newInstance(data: CommonNavigationData) = ChronologyFragment().withArgs { putParcelable(DATA_KEY, data) }
        private const val DATA_KEY = "DATA_KEY"
    }

    override val listAdapter by lazy { ChronologyAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onShowStatusDialog) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar?.apply {
            addBackButton { getPresenter().onBackPressed() }
            setTitle(R.string.common_chronology)
            inflateMenu(R.menu.menu_chronology)
            onMenuClick {
                when (it?.itemId) {
                    R.id.item_web -> getPresenter().onWebClicked()
                    R.id.item_share -> getPresenter().onShareClicked()
                }
                true
            }
        }

        emptyContentView.setText(R.string.similar_empty_description)
        fab.onClick { getPresenter().onFabClicked() }
    }

    override fun onTypeChanged(newType: ChronologyType) {
        getPresenter().onTypeChanged(newType)
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): ChronologyPresenter = chronologyPresenter

    override fun getFragmentLayout(): Int = R.layout.fragment_chronology

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun scrollTo(pos: Int) {
        postViewAction { (recyclerView?.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(pos, recyclerView.dp(16)) }
    }

    override fun showTypeDialog(currentType: ChronologyType) {
        val dialog = ChronologyTypeDialog.newInstance(currentType)
        dialog.show(childFragmentManager, "ChronologyTypeDialog")
    }
}

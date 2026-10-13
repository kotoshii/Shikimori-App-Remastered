package com.gnoemes.shikimori.presentation.view.similar

import android.os.Bundle
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.CommonNavigationData
import com.gnoemes.shikimori.presentation.presenter.similar.SimilarPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseRateButtonListFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.similar.adapter.SimilarAdapter
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import kotlinx.android.synthetic.main.layout_default_placeholders.*
import kotlinx.android.synthetic.main.layout_toolbar.*
import javax.inject.Inject

class SimilarFragment : BaseRateButtonListFragment<SimilarPresenter, SimilarView>(), SimilarView {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var similarPresenter: SimilarPresenter

    @ProvidePresenter
    fun provide() = presenterProvider.get().apply {
        localRouter = (parentFragment as RouterProvider).localRouter
        navigationData = arguments?.getParcelable(DATA_KEY)!!
    }

    companion object {
        fun newInstance(data: CommonNavigationData) = SimilarFragment().withArgs { putParcelable(DATA_KEY, data) }
        private const val DATA_KEY = "DATA_KEY"
    }

    override val listAdapter by lazy { SimilarAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onShowStatusDialog) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar?.apply {
            addBackButton { getPresenter().onBackPressed() }
            setTitle(R.string.common_similar)
        }

        emptyContentView.setText(R.string.similar_empty_description)
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): SimilarPresenter = similarPresenter

    override fun getFragmentLayout(): Int = R.layout.fragment_default_list
}

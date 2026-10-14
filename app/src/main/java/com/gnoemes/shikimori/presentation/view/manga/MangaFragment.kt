package com.gnoemes.shikimori.presentation.view.manga

import android.os.Bundle
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.common.domain.CommonNavigationData
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentType
import com.gnoemes.shikimori.presentation.presenter.manga.MangaPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.common.adapter.content.ContentAdapter
import com.gnoemes.shikimori.presentation.view.common.holders.DetailsContentViewHolder
import com.gnoemes.shikimori.presentation.view.details.BaseDetailsFragment
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.withArgs
import kotlinx.android.synthetic.main.fragment_details.*

class MangaFragment : BaseDetailsFragment<MangaPresenter, MangaView>(), MangaView {

    @InjectPresenter
    lateinit var mangaPresenter: MangaPresenter

    @ProvidePresenter
    fun providePresenter(): MangaPresenter =
            presenterProvider.get().apply {
                localRouter = (parentFragment as RouterProvider).localRouter
                val data = arguments?.getParcelable<CommonNavigationData>(AppExtras.ARGUMENT_MANGA_DATA)!!
                id = data.id
                type = data.type
            }

    companion object {
        fun newInstance(data: CommonNavigationData) = MangaFragment().withArgs { putParcelable(AppExtras.ARGUMENT_MANGA_DATA, data) }
    }

    private val charactersAdapter by lazy { ContentAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onAction) }
    private val relatedAdapter by lazy { ContentAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onAction) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoLayout.gone()
        screenshotsLayout.gone()

        contentHolders.apply {
            put(DetailsContentType.CHARACTERS, DetailsContentViewHolder(charactersLayout, charactersAdapter, true))
            put(DetailsContentType.RELATED, DetailsContentViewHolder(relatedLayout, relatedAdapter))
        }

        actionBtn.gone()
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): MangaPresenter = mangaPresenter

    override val titleRes: Int
        get() = R.string.common_manga

    override val menuRes: Int
        get() = R.menu.menu_manga
}
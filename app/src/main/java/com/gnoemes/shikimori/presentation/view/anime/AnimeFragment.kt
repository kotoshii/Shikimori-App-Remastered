package com.gnoemes.shikimori.presentation.view.anime

import android.os.Bundle
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.common.presentation.DetailsAction
import com.gnoemes.shikimori.entity.common.presentation.DetailsContentType
import com.gnoemes.shikimori.presentation.presenter.anime.AnimePresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.common.adapter.content.ContentAdapter
import com.gnoemes.shikimori.presentation.view.common.holders.DetailsContentViewHolder
import com.gnoemes.shikimori.presentation.view.details.BaseDetailsFragment
import com.gnoemes.shikimori.utils.onClick
import com.gnoemes.shikimori.utils.withArgs

class AnimeFragment : BaseDetailsFragment<AnimePresenter, AnimeView>(), AnimeView {

    @InjectPresenter
    lateinit var animePresenter: AnimePresenter

    @ProvidePresenter
    fun providePresenter(): AnimePresenter =
            presenterProvider.get().apply {
                localRouter = (parentFragment as RouterProvider).localRouter
                id = arguments!!.getLong(AppExtras.ARGUMENT_ANIME_ID)
            }

    companion object {
        fun newInstance(id: Long) = AnimeFragment().withArgs { putLong(AppExtras.ARGUMENT_ANIME_ID, id) }
    }

    private val videoAdapter by lazy { ContentAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onAction) }
    private val charactersAdapter by lazy { ContentAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onAction) }
    private val screenshotsAdapter by lazy { ContentAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onAction) }
    private val relatedAdapter by lazy { ContentAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onAction) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentHolders.apply {
            put(DetailsContentType.VIDEO, DetailsContentViewHolder(binding.videoLayout.root, videoAdapter))
            put(DetailsContentType.CHARACTERS, DetailsContentViewHolder(binding.charactersLayout.root, charactersAdapter, true))
            put(DetailsContentType.SCREENSHOTS, DetailsContentViewHolder(binding.screenshotsLayout.root, screenshotsAdapter))
            put(DetailsContentType.RELATED, DetailsContentViewHolder(binding.relatedLayout.root, relatedAdapter))
        }

        with(binding.actionBtn) {
            setImageResource(R.drawable.ic_play_arrow_filled)
            onClick { getPresenter().onAction(DetailsAction.WatchOnline()) }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): AnimePresenter = animePresenter

    override val titleRes: Int
        get() = R.string.common_anime

    override val menuRes: Int
        get() = R.menu.menu_anime
}
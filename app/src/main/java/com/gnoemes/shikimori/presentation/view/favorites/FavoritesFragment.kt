package com.gnoemes.shikimori.presentation.view.favorites

import android.os.Bundle
import android.view.View
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentFavoritesBinding
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.user.domain.FavoriteType
import com.gnoemes.shikimori.entity.user.presentation.FavoriteViewModel
import com.gnoemes.shikimori.presentation.presenter.favorites.FavoritesPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.favorites.adapter.FavoritesAdapter
import com.gnoemes.shikimori.presentation.view.favorites.holders.FavoriteCategoryViewHolder
import com.gnoemes.shikimori.utils.addBackButton
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.showCount
import com.gnoemes.shikimori.utils.withArgs
import javax.inject.Inject

class FavoritesFragment : BaseFragment<FavoritesPresenter, FavoritesView>(), FavoritesView {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var favoritePresenter: FavoritesPresenter

    @ProvidePresenter
    fun providePresenter(): FavoritesPresenter {
        return presenterProvider.get().apply {
            localRouter = (parentFragment as RouterProvider).localRouter
            id = arguments!!.getLong(AppExtras.ARGUMENT_USER_ID)
        }
    }

    companion object {
        fun newInstance(id: Long) = FavoritesFragment().withArgs { putLong(AppExtras.ARGUMENT_USER_ID, id) }
    }

    private val contentHolders = HashMap<FavoriteType, FavoriteCategoryViewHolder>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        screenToolbar?.apply {
            setTitle(R.string.common_favorite)
            addBackButton { getPresenter().onBackPressed() }
        }

        contentHolders.apply {
            put(FavoriteType.ANIME, FavoriteCategoryViewHolder(binding.animeLayout.root, defaultAdapter))
            put(FavoriteType.MANGA, FavoriteCategoryViewHolder(binding.mangaLayout.root, defaultAdapter))
            put(FavoriteType.CHARACTERS, FavoriteCategoryViewHolder(binding.charactersLayout.root, defaultAdapter))
            put(FavoriteType.SEYU, FavoriteCategoryViewHolder(binding.seyuLayout.root, defaultAdapter))
            put(FavoriteType.PRODUCERS, FavoriteCategoryViewHolder(binding.producersLayout.root, defaultAdapter))
            put(FavoriteType.MANGAKAS, FavoriteCategoryViewHolder(binding.mangakasLayout.root, defaultAdapter))
            put(FavoriteType.PEOPLE, FavoriteCategoryViewHolder(binding.otherLayout.root, defaultAdapter))
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): FavoritesPresenter = favoritePresenter

    private val binding by viewBinding(FragmentFavoritesBinding::bind)

    override fun getFragmentLayout(): Int = R.layout.fragment_favorites

    private val defaultAdapter: FavoritesAdapter
        get() = FavoritesAdapter(imageLoader, getPresenter()::onContentClicked)

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showData(items: List<FavoriteViewModel>) {
        items.forEach { contentHolders[it.type]?.bind(it) }
    }

    override fun showFavoritesCount(count: Int) {
        screenToolbar?.showCount(count)
    }
}
package com.gnoemes.shikimori.presentation.view.friends

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.user.presentation.FriendViewModel
import com.gnoemes.shikimori.presentation.presenter.friends.FriendsPresenter
import com.gnoemes.shikimori.presentation.view.base.adapter.BasePaginationAdapter
import com.gnoemes.shikimori.presentation.view.base.fragment.BasePaginationFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.friends.adapter.FriendsAdapter
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import kotlinx.android.synthetic.main.layout_default_list.*
import kotlinx.android.synthetic.main.layout_toolbar.*
import javax.inject.Inject

class FriendsFragment : BasePaginationFragment<FriendViewModel, FriendsPresenter, FriendsView>(), FriendsView {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var friendsPresenter: FriendsPresenter

    @ProvidePresenter
    fun providePresenter(): FriendsPresenter {
        return presenterProvider.get().apply {
            localRouter = (parentFragment as RouterProvider).localRouter
            id = arguments!!.getLong(AppExtras.ARGUMENT_USER_ID)
        }
    }

    companion object {
        fun newInstance(id : Long) = FriendsFragment().withArgs { putLong(AppExtras.ARGUMENT_USER_ID, id) }
    }

    private val friendsAdapter by lazy { FriendsAdapter(imageLoader, getPresenter()::onContentClicked).apply { if (!hasObservers()) setHasStableIds(true) } }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar?.apply {
            addBackButton { getPresenter().onBackPressed() }
            setTitle(R.string.common_friends)
        }

        with(recyclerView) {
            adapter = this@FriendsFragment.adapter
            layoutManager = LinearLayoutManager(context)
            addOnScrollListener(nextPageListener)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): FriendsPresenter = friendsPresenter

    override fun getFragmentLayout(): Int = R.layout.fragment_default_list

    override val adapter: BasePaginationAdapter
        get() = friendsAdapter
}

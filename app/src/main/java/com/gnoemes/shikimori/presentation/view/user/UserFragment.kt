package com.gnoemes.shikimori.presentation.view.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentUserProfileBinding
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.user.presentation.UserContentViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserHeadViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserInfoViewModel
import com.gnoemes.shikimori.entity.user.presentation.UserRateViewModel
import com.gnoemes.shikimori.presentation.presenter.user.UserPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.user.adapter.UserFavoriteContentAdapter
import com.gnoemes.shikimori.presentation.view.user.adapter.UserProfileContentAdapter
import com.gnoemes.shikimori.presentation.view.user.holders.UserContentViewHolder
import com.gnoemes.shikimori.presentation.view.user.holders.UserInfoViewHolder
import com.gnoemes.shikimori.presentation.view.user.holders.UserRateViewHolder
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.google.android.material.appbar.AppBarLayout
import javax.inject.Inject


class UserFragment : BaseFragment<UserPresenter, UserView>(), UserView {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var userPresenter: UserPresenter

    @ProvidePresenter
    fun providePresenter(): UserPresenter =
            presenterProvider.get().apply {
                localRouter = (parentFragment as RouterProvider).localRouter
                id = arguments?.getLong(AppExtras.ARGUMENT_USER_ID, Constants.NO_ID)
                        ?: Constants.NO_ID
            }

    companion object {
        fun newInstance(id: Long) = UserFragment().withArgs { putLong(AppExtras.ARGUMENT_USER_ID, id) }
        fun newInstance() = UserFragment()
    }

    private val maxHeight by lazy { (binding.appBarLayout.root.height - binding.appBarLayout.toolbar.height).toFloat() }
    private val primaryColor by lazy { context?.colorAttr(R.attr.colorPrimary)!! }

    private val favoritesAdapter by lazy { UserFavoriteContentAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onAction) }
    private val friendsAdapter by lazy { UserProfileContentAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onAction) }
    private val clubsAdapter by lazy { UserProfileContentAdapter(imageLoader, getPresenter()::onContentClicked, getPresenter()::onAction) }

    private var infoHolder: UserInfoViewHolder? = null
    private var animeRateHolder: UserRateViewHolder? = null
    private var mangaRateHolder: UserRateViewHolder? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getFragmentLayout(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appBarLayout.toolbar.apply {
            addBackButton { getPresenter().onBackPressed() }
        }

        infoHolder = UserInfoViewHolder(binding.infoLayout.root, getPresenter()::onAction)
        animeRateHolder = UserRateViewHolder(binding.animeRateLayout.root, true, getPresenter()::onAction, getPresenter()::onArrowClicked)
        mangaRateHolder = UserRateViewHolder(binding.mangaRateLayout.root, false, getPresenter()::onAction, getPresenter()::onArrowClicked)

        binding.appBarLayout.root.addOnOffsetChangedListener(appbarOffsetListener)

        binding.authLayout.signUpBtn.onClick { getPresenter().onSignUp() }
        binding.authLayout.signInBtn.onClick { getPresenter().onSignIn() }
        binding.authLayout.root.gone()
    }

    private val appbarOffsetListener = AppBarLayout.OnOffsetChangedListener { _, offset ->
        val percent = 1 - (-offset / maxHeight)

        binding.infoLayout.infoContent.lastOnlineView.alpha = percent
        binding.infoLayout.infoContent.nameView.alpha = percent
        binding.infoLayout.infoContent.avatarView.alpha = percent
        binding.appBarLayout.avatarCollapsedView.alpha = 1 - percent
        binding.appBarLayout.nameCollapsedView.alpha = 1 - percent
        val alpha = 255 - (255 * percent).toInt()
        binding.appBarLayout.toolbar.setBackgroundColor(ColorUtils.setAlphaComponent(primaryColor, if (alpha < 0) 0 else if (alpha > 255) 255 else alpha))
    }

    //before super: the binding goes with the view
    override fun onDestroyView() {
        binding.appBarLayout.root.removeOnOffsetChangedListener(appbarOffsetListener)
        super.onDestroyView()
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): UserPresenter = userPresenter

    private val binding by viewBinding(FragmentUserProfileBinding::bind)

    override fun getFragmentLayout(): Int = R.layout.fragment_user_profile

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun setHead(data: UserHeadViewModel) {
        binding.infoLayout.infoContent.lastOnlineView.text = data.lastOnline
        binding.infoLayout.infoContent.nameView.text = data.name
        binding.appBarLayout.nameCollapsedView.text = data.name

        imageLoader.setCircleImage(binding.infoLayout.infoContent.avatarView, data.image.x160)
        imageLoader.setCircleImage(binding.appBarLayout.avatarCollapsedView, data.image.x160)
    }

    override fun setInfo(data: UserInfoViewModel) {
        infoHolder?.bind(data)
    }

    override fun setAnimeRate(data: UserRateViewModel) {
        animeRateHolder?.bind(data)
    }

    override fun setMangaRate(data: UserRateViewModel) {
        mangaRateHolder?.bind(data)
    }

    override fun setFavorites(isMe: Boolean, it: UserContentViewModel) {
        val layout = if (isMe) binding.thirdContentLayout else binding.firstContentLayout
        UserContentViewHolder(layout.root, favoritesAdapter).bind(it)
    }

    override fun setFriends(isMe: Boolean, it: UserContentViewModel) {
        val layout = if (isMe) binding.firstContentLayout else binding.secondContentLayout
        UserContentViewHolder(layout.root, friendsAdapter).bind(it)
    }

    override fun setClubs(isMe: Boolean, it: UserContentViewModel) {
        val layout = if (isMe) binding.secondContentLayout else binding.thirdContentLayout
        UserContentViewHolder(layout.root, clubsAdapter).bind(it)
    }

    override fun resetProfile() {
        binding.appBarLayout.root.setExpanded(true, false)
        binding.scrollView.scrollTo(0, 0)
        binding.appBarLayout.nameCollapsedView.text = null
        imageLoader.clearImage(binding.appBarLayout.avatarCollapsedView)
        infoHolder?.reset()
        animeRateHolder?.reset()
        mangaRateHolder?.reset()
        listOf(binding.firstContentLayout, binding.secondContentLayout, binding.thirdContentLayout).forEach { UserContentViewHolder.reset(it.root) }
    }

    override fun showContent(show: Boolean) {
        binding.scrollView.visibleIf { show }
        if (binding.appBarLayout.toolbar.navigationIcon == null) binding.appBarLayout.root.visible()
        else binding.appBarLayout.root.visibleIf { show }
    }

    override fun toggleAnimeRate(expanded: Boolean) {
        animeRateHolder?.toggle(expanded)
    }

    override fun toggleMangaRate(expanded: Boolean) {
        mangaRateHolder?.toggle(expanded)
    }

    override fun addSettings() {
        with(binding.appBarLayout.toolbar) {
            navigationIcon = null
            inflateMenu(R.menu.menu_user)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.item_settings -> getPresenter().onSettingsClicked()
                    R.id.item_sign_out -> getPresenter().onSignOutClicked()
                }
                true
            }
        }
    }

    override fun showSignOut(show: Boolean) {
        binding.appBarLayout.toolbar.menu?.findItem(R.id.item_sign_out)?.isVisible = show
    }

    override fun showSignOutDialog() {
        context!!.materialDialog().show {
            message(R.string.user_sign_out_confirm)
            positiveButton(R.string.user_sign_out) { getPresenter().onSignOutConfirmed() }
            negativeButton(R.string.common_cancel)
        }
    }

    override fun showAuthView(show: Boolean) {
        binding.authLayout.root.visibleIf { show }
        binding.appBarLayout.root.visible()
    }

    override fun showNetworkView() {
        screenErrorView?.visible()
    }

    override fun hideNetworkView() {
        screenErrorView?.gone()
    }
}
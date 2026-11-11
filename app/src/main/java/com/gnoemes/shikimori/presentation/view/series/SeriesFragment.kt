package com.gnoemes.shikimori.presentation.view.series

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentSeriesBinding
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.common.domain.Screens
import com.gnoemes.shikimori.entity.series.domain.PlayerType
import com.gnoemes.shikimori.entity.series.domain.Track
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.presentation.*
import com.gnoemes.shikimori.presentation.presenter.series.SeriesPresenter
import com.gnoemes.shikimori.presentation.view.series.download.SeriesDownloadDialog
import com.gnoemes.shikimori.presentation.view.series.download.SeriesDownloadHostingDialog
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.common.fragment.DescriptionDialogFragment
import com.gnoemes.shikimori.presentation.view.series.episodes.EpisodesFragment
import com.gnoemes.shikimori.presentation.view.series.translations.adapter.TranslationsAdapter
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.navigation.RouteHolder
import com.gnoemes.shikimori.utils.navigation.startOutsideActivity
import com.gnoemes.shikimori.utils.widgets.VerticalSpaceItemDecorator
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class SeriesFragment : BaseFragment<SeriesPresenter, SeriesView>(),
        SeriesView,
        PlayerSelectDialog.Callback,
        QualitySelectDialog.Callback,
        EpisodesFragment.EpisodesCallback,
        SeriesDownloadDialog.SeriesDownloadCallback,
        SeriesDownloadHostingDialog.Callback,
        HasSupportFragmentInjector {

    @Inject
    lateinit var imageLoader: ImageLoader

    @InjectPresenter
    lateinit var seriesPresenter: SeriesPresenter

    @Inject
    lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = childFragmentInjector

    @ProvidePresenter
    fun providePresenter(): SeriesPresenter = presenterProvider.get().apply {
        localRouter = (parentFragment as RouterProvider).localRouter
        navigationData = arguments?.getParcelable(SERIES_NAVIGATION_DATA)!!
        restoredState = this@SeriesFragment.restoredState
    }

    companion object {
        fun newInstance(data: SeriesNavigationData) = SeriesFragment().withArgs { putParcelable(SERIES_NAVIGATION_DATA, data) }
        private const val SERIES_NAVIGATION_DATA = "SERIES_NAVIGATION_DATA"
        private const val SERIES_STATE = "SERIES_STATE"
    }

    //read before the presenter is made, which happens inside super.onCreate. After a rotation Moxy
    //keeps the presenter, and only after Android killed the app is a new one made from this
    private var restoredState: SeriesState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        restoredState = savedInstanceState?.getParcelable(SERIES_STATE)
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        getPresenter().state?.let { outState.putParcelable(SERIES_STATE, it) }
    }

    private val defaultCorners by lazy { context!!.dimen(R.dimen.margin_big) }

    //the toolbar as the presenter last set it (SeriesView.showControls), and the picker open over it
    private var isSourceShown = false
    private var shownType: TranslationType? = null
    private var episodeControls: EpisodeControls? = null
    private var openPicker: Picker? = null

    private enum class Picker { SOURCES, TYPES }

    private val adapter by lazy {
        TranslationsAdapter(getPresenter()::onHostingClicked, ::copyHostingUrl, getPresenter()::onMenuClicked)
    }

    /**
     * Copying a link changes no state and needs a `Context`, so it is done here rather than routed
     * through the presenter - the same way `EpisodesFragment` shows its own messages.
     */
    private fun copyHostingUrl(video: TranslationVideo) {
        val url = video.webPlayerUrl
        if (url.isNullOrBlank()) return

        val context = context ?: return
        context.clipboardManager().setPrimaryClip(ClipData.newPlainText(video.videoHosting.synonymType, url))

        //Always shown. This was skipped on api 33+ on the assumption that the system's own
        //"copied" confirmation would cover it, but on Android 16 nothing appeared at all - the
        //system ui does not show for an app targeting 28. Silence is worse than a possible double.
        Toast.makeText(context, R.string.series_link_copied, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(getFragmentLayout(), container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.appBarLayout.toolbar) {
            inflateMenu(R.menu.menu_series)
            setOnMenuItemClickListener { getPresenter().onSearchClicked(); true }
            addBackButton { getPresenter().onBackPressed() }
        }

        binding.appBarLayout.searchToolbar.addBackButton { getPresenter().onSearchClose() }

        with(binding.recyclerView) {
            adapter = this@SeriesFragment.adapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(VerticalSpaceItemDecorator(context.dimen(R.dimen.margin_normal).toInt(), false))
            setHasFixedSize(true)
            addOnScrollListener(shadowScrollListener)
        }

        with(binding.appBarLayout.searchView) {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    hideSoftInput()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    getPresenter().onQueryChanged(newText)
                    return true
                }
            })
            applyAppStyle()
        }

        screenEmptyView?.setText(R.string.episodes_not_found)
        binding.authorsLayout.root.gone()
        binding.appbar.translationBtn.gone()
        binding.progress.visible()

        binding.appbar.translationBtn.onClick { openPicker(Picker.TYPES) }
        binding.appbar.voiceBtn.onClick { onTypeSelected(TranslationType.VOICE_RU) }
        binding.appbar.subtitlesBtn.onClick { onTypeSelected(TranslationType.SUB_RU) }
        binding.appbar.originalBtn.onClick { onTypeSelected(TranslationType.RAW) }

        binding.appbar.episodeChip.onClick { getPresenter().showEpisodes() }
        binding.appbar.sourceChangeBtn.onClick { openPicker(Picker.SOURCES) }

        binding.appbar.mainSource.onClick { onSourceSelected(false) }
        binding.appbar.altSource.onClick { onSourceSelected(true) }

        binding.authorsLayout.actionBtn.onClick { onSourceSelected(true) }
        binding.fab.onClick { getPresenter().onDiscussionClicked() }
        binding.appbar.nextEpisodeBtn.onClick { getPresenter().onNextEpisode() }
        binding.appbar.previousEpisodeBtn.onClick { getPresenter().onPreviousEpisode() }
    }

    private fun onTypeSelected(newType: TranslationType) {
        getPresenter().onTypeChanged(newType)
        openPicker(null)
    }

    private fun onSourceSelected(isAlternative: Boolean) {
        getPresenter().onSourceChanged(isAlternative)
        openPicker(null)
    }

    private fun openPicker(picker: Picker?) {
        openPicker = picker
        showToolbar(AutoTransition())
    }

    //the one place that decides what the toolbar shows: the presenter's controls, with an open
    //picker in place of the buttons it covers
    private fun showToolbar(transition: Transition) {
        TransitionManager.beginDelayedTransition(binding.appbar.motionLayout, transition)
        val sources = openPicker == Picker.SOURCES
        val types = openPicker == Picker.TYPES
        val episodes = episodeControls
        binding.appbar.episodeChip.visibleIf { episodes != null && !sources }
        binding.appbar.previousEpisodeBtn.visibleIf { episodes?.hasArrows == true && !sources }
        binding.appbar.nextEpisodeBtn.visibleIf { episodes?.hasArrows == true && !sources }
        if (episodes != null) {
            binding.appbar.episodeChip.setUsable(episodes.isEnabled)
            binding.appbar.previousEpisodeBtn.setUsable(episodes.isEnabled && episodes.canGoBack)
            binding.appbar.nextEpisodeBtn.setUsable(episodes.isEnabled && episodes.canGoForward)
        }
        binding.appbar.sourceChangeBtn.visibleIf { isSourceShown && !sources }
        binding.appbar.mainSource.visibleIf { sources }
        binding.appbar.altSource.visibleIf { sources }
        binding.appbar.translationBtn.visibleIf { shownType != null && !types }
        binding.appbar.voiceBtn.visibleIf { types }
        binding.appbar.subtitlesBtn.visibleIf { types }
        binding.appbar.originalBtn.visibleIf { types }
    }

    private val shadowScrollListener = object : RecyclerView.OnScrollListener() {
        private val shadowDefault by lazy { context!!.dimen(R.dimen.default_shadow) }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (recyclerView.layoutManager != null) {

                if (recyclerView.canScrollVertically(-1)) ViewCompat.setElevation(binding.appbar.root, shadowDefault)
                else ViewCompat.setElevation(binding.appbar.root, 0f)
            }
        }
    }

    //before super: the binding goes with the view
    override fun onDestroyView() {
        binding.recyclerView.removeOnScrollListener(shadowScrollListener)
        super.onDestroyView()
    }

    override fun onPlayerSelected(playing: EmbeddedPlayerNavigationData, playerType: PlayerType, remember: Boolean) =
            getPresenter().onPlayerSelected(playing, playerType, remember)

    override fun onQualitySelected(video: TranslationVideo, playerType: PlayerType, parsed: Video, track: Track) =
            getPresenter().onQualitySelected(video, playerType, parsed, track)

    override fun onDownload(item: SeriesDownloadItem) = getPresenter().onTrackForDownloadSelected(item)
    override fun onShare(item: SeriesDownloadItem) = getPresenter().onShare(item)
    override fun onDownloadHostingSelected(video: TranslationVideo) = getPresenter().onDownloadHostingSelected(video)

    override fun onEpisodeSelected(episodeId: Long, episode: Int, isAlternative: Boolean) =
            getPresenter().onEpisodeSelected(episodeId, episode, isAlternative)

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): SeriesPresenter = seriesPresenter

    private val binding by viewBinding(FragmentSeriesBinding::bind)

    override fun getFragmentLayout(): Int = R.layout.fragment_series

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showData(newItems: List<TranslationViewModel>) {
        adapter.bindItems(newItems)
    }

    override fun setTitle(title: String) {
        binding.appBarLayout.toolbar.title = title
    }

    override fun showEmptyAuthorsView(show: Boolean, isAlternative: Boolean) {
        if (show) showEmptyState(R.string.series_empty_authors_title, R.string.series_empty_authors_description, !isAlternative)
        else binding.authorsLayout.root.gone()
    }

    override fun showEpisodeNotFoundView(isAlternative: Boolean) =
            showEmptyState(R.string.series_empty_episode_title, R.string.series_empty_episode_description, !isAlternative)

    //the place under the toolbar for a list with nothing to show; its button switches to the other source
    private fun showEmptyState(@StringRes title: Int, @StringRes description: Int, isSourceButtonShown: Boolean) {
        binding.authorsLayout.root.visible()
        binding.authorsLayout.titleView.setText(title)
        binding.authorsLayout.descriptionView.setText(description)
        binding.authorsLayout.actionBtn.visibleIf { isSourceButtonShown }
    }

    override fun showEmptySearchView() {
        val emptyItem = SeriesPlaceholderItem(R.string.translation_search_empty_title, R.string.translation_search_empty_desc)
        adapter.bindItems(mutableListOf(emptyItem))
    }

    override fun showControls(isSourceShown: Boolean, type: TranslationType?, episodes: EpisodeControls?) {
        this.isSourceShown = isSourceShown
        shownType = type
        episodeControls = episodes
        //a picker whose button went away closes with it
        if (openPicker == Picker.SOURCES && !isSourceShown || openPicker == Picker.TYPES && type == null) openPicker = null

        val icon = when (type) {
            TranslationType.VOICE_RU -> R.drawable.ic_voice
            TranslationType.SUB_RU -> R.drawable.ic_subs
            TranslationType.RAW -> R.drawable.ic_original
            else -> 0
        }
        if (icon != 0) binding.appbar.translationBtn.setIconResource(icon)

        //«#» lighter than the number, at Material's 60% for secondary text. It was the chip's own
        //background colour, and could hardly be seen (BUGS #74)
        val hashColor = ColorUtils.setAlphaComponent(context!!.colorAttr(R.attr.colorSecondary), 153)
        if (episodes != null) binding.appbar.episodeChip.text = "# ".colorSpan(hashColor).append("${episodes.episode}")

        showToolbar(Fade())
    }

    override fun changeSource(isAlternative: Boolean) {
        binding.appbar.mainSource.setTextColor(context!!.colorAttr(if (isAlternative) R.attr.colorOnSurface else R.attr.colorSecondary))
        binding.appbar.altSource.setTextColor(context!!.colorAttr(if (isAlternative) R.attr.colorSecondary else R.attr.colorOnSurface))
    }

    override fun showSearchView() {
        TransitionManager.beginDelayedTransition(binding.appBarLayout.root, Fade())
        binding.appBarLayout.searchToolbar.visible()
        binding.appBarLayout.toolbar.gone()
        binding.appbar.motionLayout.gone()
        binding.backdrop.radius = 0f
    }

    //as the characters search and the episode sheet do: focus and keyboard
    override fun focusSearch() {
        binding.appBarLayout.searchView.isIconified = false
    }

    override fun onSearchClosed() {
        TransitionManager.beginDelayedTransition(binding.appBarLayout.root, Fade())
        binding.appbar.motionLayout.visible()
        binding.appBarLayout.toolbar.visible()
        binding.appBarLayout.searchToolbar.gone()
        binding.backdrop.radius = defaultCorners
    }

    override fun showPlayerDialog(playing: EmbeddedPlayerNavigationData) {
        val dialog = PlayerSelectDialog.newInstance(playing)
        dialog.show(childFragmentManager, "PlayerSelect")
    }

    override fun showDownloadHostingDialog(title: String, videos: List<TranslationVideo>, hasUnsupported: Boolean) {
        val dialog = SeriesDownloadHostingDialog.newInstance(title, videos, hasUnsupported)
        dialog.show(childFragmentManager, "DownloadHostingDialog")
    }

    override fun showDownloadDialog(title: String, items: List<SeriesDownloadItem>) {
        val dialog = SeriesDownloadDialog.newInstance(title, items)
        dialog.show(childFragmentManager, "DownloadDialog")
    }

    override fun showAuthorDialog(author: String) {
        DescriptionDialogFragment.newInstance(titleRes = R.string.translation_authors, text = author)
                .show(childFragmentManager, "AuthorsDialog")
    }

    override fun showEpisodesDialog(data: EpisodesNavigationData) {
        val dialog = EpisodesFragment.newInstance(data)
        dialog.show(childFragmentManager, "EpisodeDialog")
    }

    override fun showTracksNotFoundError() {
        Toast.makeText(requireContext(), R.string.series_tracks_empty, Toast.LENGTH_SHORT).show()
    }

    override fun showAnime365LoginRequired() {
        Toast.makeText(requireContext(), R.string.series_anime_365_login_required, Toast.LENGTH_LONG).show()
    }

    override fun showHostingChallengeError() {
        Toast.makeText(requireContext(), R.string.series_hosting_challenge, Toast.LENGTH_LONG).show()
    }

    override fun checkPermissions() = activity!!.askStoragePermission { getPresenter().onStoragePermissionsAccepted() }

    override fun showFolderChooserDialog() = context!!.chooseDownloadFolder(onPositive = { getPresenter().onStoragePermissionsAccepted() })

    //opened here, not through the router: only here is it known whether an app took it
    override fun openExternalPlayer(video: TranslationVideo, data: ExternalPlayerData) {
        val intent = RouteHolder.createActivityIntent(context, Screens.EXTERNAL_PLAYER, data) ?: return
        //said first, so it also explains «Нет приложения…» when no app takes a DASH manifest (BUGS #56)
        if (data.isDash) Toast.makeText(requireContext(), R.string.series_external_dash_hint, Toast.LENGTH_LONG).show()
        if (requireContext().startOutsideActivity(intent)) getPresenter().onExternalPlayerOpened(video)
    }

    override fun showQualityChooser(video: TranslationVideo, playerType: PlayerType, parsed: Video) {
        val dialog = QualitySelectDialog.newInstance(video, playerType, parsed)
        dialog.show(childFragmentManager, "QualityDialog")
    }

    //nothing is out at all: no source can help
    override fun showEmptyView() =
            showEmptyState(R.string.series_empty_episode_title, R.string.series_empty_episode_description, false)

    override fun showContent(show: Boolean) = binding.recyclerView.visibleIf { show }
    override fun hideEmptyView() { screenEmptyView?.gone() }
    override fun showNetworkView() { screenErrorView?.visible() }
    override fun hideNetworkView() { screenErrorView?.gone() }
    override fun setBackground(image: Image) = imageLoader.setBlurredImage(binding.backgroundImage, image.original, sampling = 2)
    override fun scrollToPosition(position: Int) = binding.recyclerView.scrollToPosition(position)
    override fun showFab(show: Boolean) = if (show) binding.fab.show() else binding.fab.hide()
    override fun onShowLoading() = binding.progress.visible()
    override fun onHideLoading() = binding.progress.gone()
    override fun onShowLightLoading() = binding.progress.visible()
    override fun onHideLightLoading() = binding.progress.gone()
}
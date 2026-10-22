package com.gnoemes.shikimori.presentation.view.series

import android.content.ClipData
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.SearchView
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
import com.gnoemes.shikimori.entity.common.domain.Image
import com.gnoemes.shikimori.entity.series.domain.PlayerType
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import com.gnoemes.shikimori.entity.series.presentation.*
import com.gnoemes.shikimori.presentation.presenter.series.SeriesPresenter
import com.gnoemes.shikimori.presentation.view.series.download.SeriesDownloadDialog
import com.gnoemes.shikimori.presentation.view.series.download.SeriesDownloadHostingDialog
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragment
import com.gnoemes.shikimori.presentation.view.base.fragment.RouterProvider
import com.gnoemes.shikimori.presentation.view.common.fragment.DescriptionDialogFragment
import com.gnoemes.shikimori.presentation.view.common.fragment.ListDialogFragment
import com.gnoemes.shikimori.presentation.view.series.episodes.EpisodesFragment
import com.gnoemes.shikimori.presentation.view.series.translations.adapter.TranslationsAdapter
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import com.gnoemes.shikimori.utils.widgets.VerticalSpaceItemDecorator
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.fragment_series.*
import kotlinx.android.synthetic.main.layout_default_placeholders.*
import kotlinx.android.synthetic.main.layout_series_empty_authors.*
import kotlinx.android.synthetic.main.layout_series_toolbar.*
import kotlinx.android.synthetic.main.layout_toolbar_transparent_with_search.*
import javax.inject.Inject

class SeriesFragment : BaseFragment<SeriesPresenter, SeriesView>(),
        SeriesView,
        PlayerSelectDialog.Callback,
        ListDialogFragment.DialogCallback,
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
    }

    companion object {
        fun newInstance(data: SeriesNavigationData) = SeriesFragment().withArgs { putParcelable(SERIES_NAVIGATION_DATA, data) }
        private const val SERIES_NAVIGATION_DATA = "SERIES_NAVIGATION_DATA"
    }

    private val defaultCorners by lazy { context!!.dimen(R.dimen.margin_big) }

    //the toolbar as the presenter last set it (SeriesView.showControls), and the picker open over it
    private var isSourceShown = false
    private var shownType: TranslationType? = null
    private var shownEpisode: Int? = null
    private var isNextShown = false
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
        context.clipboardManager().primaryClip = ClipData.newPlainText(video.videoHosting.synonymType, url)

        //Always shown. This was skipped on api 33+ on the assumption that the system's own
        //"copied" confirmation would cover it, but on Android 16 nothing appeared at all - the
        //system ui does not show for an app targeting 28. Silence is worse than a possible double.
        Toast.makeText(context, R.string.series_link_copied, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = inflater.inflate(getFragmentLayout(), container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(toolbar) {
            inflateMenu(R.menu.menu_series)
            setOnMenuItemClickListener { getPresenter().onSearchClicked(); true }
            addBackButton { getPresenter().onBackPressed() }
        }

        searchToolbar.addBackButton { getPresenter().onSearchClose() }

        with(recyclerView) {
            adapter = this@SeriesFragment.adapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(VerticalSpaceItemDecorator(context.dimen(R.dimen.margin_normal).toInt(), false))
            setHasFixedSize(true)
            addOnScrollListener(shadowScrollListener)
        }

        with(searchView) {
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

        emptyContentView.setText(R.string.episodes_not_found)
        networkErrorView.callback = { getPresenter().onRefresh() }
        networkErrorView.showButton()

        authorsLayout.gone()
        translationBtn.gone()
        progress.visible()

        translationBtn.onClick { openPicker(Picker.TYPES) }
        voiceBtn.onClick { onTypeSelected(TranslationType.VOICE_RU) }
        subtitlesBtn.onClick { onTypeSelected(TranslationType.SUB_RU) }
        originalBtn.onClick { onTypeSelected(TranslationType.RAW) }

        episodeChip.onClick { getPresenter().showEpisodes() }
        sourceChangeBtn.onClick { openPicker(Picker.SOURCES) }

        mainSource.onClick { onSourceSelected(false) }
        altSource.onClick { onSourceSelected(true) }

        actionBtn.onClick { onSourceSelected(true) }
        fab.onClick { getPresenter().onDiscussionClicked() }
        nextEpisodeBtn.onClick { getPresenter().onNextEpisode() }
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
        TransitionManager.beginDelayedTransition(motionLayout, transition)
        val sources = openPicker == Picker.SOURCES
        val types = openPicker == Picker.TYPES
        episodeChip.visibleIf { shownEpisode != null && !sources }
        nextEpisodeBtn.visibleIf { isNextShown && !sources }
        sourceChangeBtn.visibleIf { isSourceShown && !sources }
        mainSource.visibleIf { sources }
        altSource.visibleIf { sources }
        translationBtn.visibleIf { shownType != null && !types }
        voiceBtn.visibleIf { types }
        subtitlesBtn.visibleIf { types }
        originalBtn.visibleIf { types }
    }

    private val shadowScrollListener = object : RecyclerView.OnScrollListener() {
        private val shadowDefault by lazy { context!!.dimen(R.dimen.default_shadow) }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (recyclerView.layoutManager != null) {

                if (recyclerView.canScrollVertically(-1)) ViewCompat.setElevation(appbar, shadowDefault)
                else ViewCompat.setElevation(appbar, 0f)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.removeOnScrollListener(shadowScrollListener)
    }

    override fun onPlayerSelected(playerType: PlayerType) {
        getPresenter().onPlayerSelected(playerType)
    }

    override fun dialogItemCallback(tag: String?, action: String) {
        if (tag == "QualityDialog") {
            getPresenter().onQualityChoosed(action)
        }
    }

    override fun onDownload(item: SeriesDownloadItem) = getPresenter().onTrackForDownloadSelected(item)
    override fun onShare(item: SeriesDownloadItem) = getPresenter().onShare(item)
    override fun onDownloadHostingSelected(video: TranslationVideo) = getPresenter().onDownloadHostingSelected(video)

    override fun onEpisodeSelected(episodeId: Long, episode: Int, isAlternative: Boolean) =
            getPresenter().onEpisodeSelected(episodeId, episode, isAlternative)

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getPresenter(): SeriesPresenter = seriesPresenter

    override fun getFragmentLayout(): Int = R.layout.fragment_series

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showData(newItems: List<TranslationViewModel>) {
        adapter.bindItems(newItems)
    }

    override fun setTitle(title: String) {
        toolbar?.title = title
    }

    override fun showEmptyAuthorsView(show: Boolean, isAlternative: Boolean) {
        authorsLayout.visibleIf { show }
        titleView.setText(R.string.series_empty_authors_title)
        descriptionView.setText(R.string.series_empty_authors_description)
        if (isAlternative) actionBtn.gone()
        else actionBtn.visible()
    }

    override fun showEmptySearchView() {
        val emptyItem = SeriesPlaceholderItem(R.string.translation_search_empty_title, R.string.translation_search_empty_desc)
        adapter.bindItems(mutableListOf(emptyItem))
    }

    override fun showControls(isSourceShown: Boolean, type: TranslationType?, episode: Int?, isNextShown: Boolean) {
        this.isSourceShown = isSourceShown
        shownType = type
        shownEpisode = episode
        this.isNextShown = isNextShown
        //a picker whose button went away closes with it
        if (openPicker == Picker.SOURCES && !isSourceShown || openPicker == Picker.TYPES && type == null) openPicker = null

        val icon = when (type) {
            TranslationType.VOICE_RU -> R.drawable.ic_voice
            TranslationType.SUB_RU -> R.drawable.ic_subs
            TranslationType.RAW -> R.drawable.ic_original
            else -> 0
        }
        if (icon != 0) translationBtn.setIconResource(icon)

        if (episode != null) episodeChip.text = "# ".colorSpan(context!!.colorAttr(R.attr.colorSecondaryTransparent)).append("$episode")

        showToolbar(Fade())
    }

    override fun changeSource(isAlternative: Boolean) {
        mainSource.setTextColor(context!!.colorAttr(if (isAlternative) R.attr.colorOnSurface else R.attr.colorSecondary))
        altSource.setTextColor(context!!.colorAttr(if (isAlternative) R.attr.colorSecondary else R.attr.colorOnSurface))
    }

    override fun showSearchView() {
        TransitionManager.beginDelayedTransition(appBarLayout, Fade())
        searchToolbar.visible()
        toolbar.gone()
        motionLayout.gone()
        backdrop.radius = 0f
    }

    override fun onSearchClosed() {
        TransitionManager.beginDelayedTransition(appBarLayout, Fade())
        motionLayout.visible()
        toolbar.visible()
        searchToolbar.gone()
        backdrop.radius = defaultCorners
    }

    override fun showPlayerDialog() {
        val dialog = PlayerSelectDialog.newInstance()
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

    override fun showQualityChooser(items: List<Pair<String, String>>) {
        val dialog = ListDialogFragment.newInstance()
        dialog.apply {
            setItems(items)
        }.show(childFragmentManager, "QualityDialog")
    }

    override fun showEmptyView() {
        authorsLayout.visible()
        actionBtn.gone()
        titleView.setText(R.string.series_empty_episode_title)
        descriptionView.setText(R.string.series_empty_episode_description)
    }

    override fun showContent(show: Boolean) = recyclerView.visibleIf { show }
    override fun hideEmptyView() = emptyContentView.gone()
    override fun showNetworkView() = networkErrorView.visible()
    override fun hideNetworkView() = networkErrorView.gone()
    override fun setBackground(image: Image) = imageLoader.setBlurredImage(backgroundImage, image.original, sampling = 2)
    override fun scrollToPosition(position: Int) = recyclerView.scrollToPosition(position)
    override fun showFab(show: Boolean) = if (show) fab.show() else fab.hide()
    override fun onShowLoading() = progress.visible()
    override fun onHideLoading() = progress.gone()
    override fun onShowLightLoading() = progress.visible()
    override fun onHideLightLoading() = progress.gone()
}
package com.gnoemes.shikimori.presentation.view.series.episodes

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.presenter.ProvidePresenter
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.FragmentEpisodesBinding
import com.gnoemes.shikimori.databinding.LayoutDefaultPlaceholdersBinding
import com.gnoemes.shikimori.entity.app.domain.AppExtras
import com.gnoemes.shikimori.entity.series.presentation.EpisodePlaceholderItem
import com.gnoemes.shikimori.entity.series.presentation.EpisodeViewModel
import com.gnoemes.shikimori.entity.series.presentation.EpisodesNavigationData
import com.gnoemes.shikimori.entity.series.presentation.SeriesPlaceholderItem
import com.gnoemes.shikimori.presentation.presenter.series.episodes.EpisodesPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseBottomSheetInjectionDialogFragment
import com.gnoemes.shikimori.presentation.view.common.fragment.ListDialogFragment
import com.gnoemes.shikimori.presentation.view.series.episodes.adapter.EpisodeAdapter
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.widgets.VerticalSpaceItemDecorator

class EpisodesFragment : BaseBottomSheetInjectionDialogFragment<EpisodesPresenter, EpisodesView>(), EpisodesView, ListDialogFragment.DialogCallback {

    @InjectPresenter
    lateinit var episodesPresenter: EpisodesPresenter

    @ProvidePresenter
    fun providePresenter(): EpisodesPresenter {
        return presenterProvider.get().apply {
            navigationData = arguments?.getParcelable(AppExtras.ARGUMENT_EPISODES_DATA)!!
        }
    }

    companion object {
        fun newInstance(data: EpisodesNavigationData) = EpisodesFragment().withArgs { putParcelable(AppExtras.ARGUMENT_EPISODES_DATA, data) }
        private const val CHECK_ALL_PREVIOUS_ACTION = "check_all_previous_"
    }

    private val adapter by lazy { EpisodeAdapter(presenter::onEpisodeClicked, presenter::onEpisodeLongClick) }

    override val useWrapperLayout = false

    private val binding by viewBinding(FragmentEpisodesBinding::bind)

    //layout_default_placeholders is a <merge>: its views sit straight in the sheet's layout
    private val placeholders by viewBinding(LayoutDefaultPlaceholdersBinding::bind)

    override val networkErrorView get() = placeholders.networkErrorView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureSearchView()

        binding.toolbar.apply {
            addBackButton(R.drawable.ic_close) { onBackPressed() }
            setTitle(R.string.common_episodes)
            inflateMenu(R.menu.menu_episodes)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.item_alternative_source -> presenter.onAlternativeSourceClicked()
                    R.id.item_search -> presenter.onSearchClicked()
                }
                true
            }
        }

        binding.searchToolbar.addBackButton { presenter.onSearchClosed() }
        binding.episodesLayout.actionBtn.onClick { presenter.onAlternativeSourceClicked() }

        with(binding.recyclerView) {
            adapter = this@EpisodesFragment.adapter
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(VerticalSpaceItemDecorator(context.dimen(R.dimen.margin_normal).toInt(), true))
            setHasFixedSize(true)
        }

        placeholders.emptyContentView.setText(R.string.episodes_not_found)
        //the sheet has «Повторить», not a pull to refresh
        placeholders.networkErrorView.setText(R.string.common_error_message_without_pull)
        placeholders.networkErrorView.showButton()
        placeholders.networkErrorView.callback = { presenter.onRefresh() }
        placeholders.emptyContentView.gone()
        placeholders.networkErrorView.gone()
        binding.episodesLayout.root.gone()
    }

    private fun configureSearchView() {
        with(binding.searchView) {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    hideSoftInput()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    presenter.onQueryChanged(newText)
                    return true
                }
            })
            applyAppStyle()
        }
    }

    override fun dialogItemCallback(tag: String?, action: String) {
        if (!tag.isNullOrBlank() && tag == "OptionsTag") {
            if (action.contains(CHECK_ALL_PREVIOUS_ACTION)) presenter.onCheckAllPrevious(action.replace(CHECK_ALL_PREVIOUS_ACTION, "").toInt())
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override val presenter: EpisodesPresenter
        get() = episodesPresenter

    override fun getDialogLayout(): Int = R.layout.fragment_episodes

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showData(newItems: List<EpisodeViewModel>) {
        adapter.bindItems(newItems)
    }

    override fun scrollToPosition(pos: Int) {
        (binding.recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(pos, 0)
    }

    override fun showSearchView() {
        TransitionManager.beginDelayedTransition(binding.appBarLayout, Fade())
        binding.searchToolbar.visible()
        binding.toolbar.gone()
        binding.searchView.isIconified = false
    }

    override fun hideSearchView() {
        TransitionManager.beginDelayedTransition(binding.appBarLayout, Fade())
        binding.searchToolbar.gone()
        binding.toolbar.visible()
        hideSoftInput()
    }

    override fun onShowLoading() {
        val items = (1..12).map { EpisodePlaceholderItem(it) }
        adapter.bindItems(items)
    }

    override fun onHideLoading() = Unit

    override fun showEmptyEpisodesView(show: Boolean, isAlternative: Boolean) {
        binding.episodesLayout.root.visibleIf { show }
        binding.episodesLayout.titleView.setText(R.string.episodes_empty_title)
        //the source shown: it said «В основном источнике» on the alternative one too
        binding.episodesLayout.descriptionView.setText(if (isAlternative) R.string.episodes_empty_description_alternative else R.string.episodes_empty_description)
        binding.episodesLayout.actionBtn.visibleIf { !isAlternative }
    }

    override fun showAlternativeLabel(show: Boolean) {
        if (show) binding.toolbar.setSubtitle(R.string.series_alternative_source)
        else binding.toolbar.subtitle = null
        binding.toolbar.setTitleTextAppearance(context, if (show) R.style.ToolbarSmallTextAppearance else R.style.ToolbarTextAppearance)
    }

    override fun showEpisodeOptionsDialog(index: Int) {
        val dialog = ListDialogFragment.newInstance()
        val items = mutableListOf<Pair<String, String>>()
                .apply {
                    add(Pair(context!!.getString(R.string.episode_check_all_previous) + " $index", CHECK_ALL_PREVIOUS_ACTION + index))
                }
        dialog.apply {
            setItems(items)
        }.show(childFragmentManager, "OptionsTag")
    }

    override fun showContent(show: Boolean) = binding.recyclerView.visibleIf { show }

    override fun showSearchEmpty() {
        val item = SeriesPlaceholderItem(R.string.episode_search_empty_title, R.string.episode_search_empty_desc)
        adapter.bindItems(mutableListOf(item))
    }

    override fun onEpisodeSelected(episodeId: Long, episode: Int, isAlternative: Boolean) {
        (parentFragment as? EpisodesCallback)?.onEpisodeSelected(episodeId, episode, isAlternative)
        onBackPressed()
    }

    interface EpisodesCallback {
        fun onEpisodeSelected(episodeId: Long, episode: Int, isAlternative: Boolean)
    }
}
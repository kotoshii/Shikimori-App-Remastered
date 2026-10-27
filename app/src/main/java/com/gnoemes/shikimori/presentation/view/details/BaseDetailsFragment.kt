package com.gnoemes.shikimori.presentation.view.details

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.common.domain.Link
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.common.presentation.*
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.entity.user.presentation.UserStatisticItem
import com.gnoemes.shikimori.presentation.presenter.details.BaseDetailsPresenter
import com.gnoemes.shikimori.presentation.view.base.fragment.BaseFragment
import com.gnoemes.shikimori.presentation.view.common.adapter.ActionAdapter
import com.gnoemes.shikimori.presentation.view.common.adapter.InfoAdapter
import com.gnoemes.shikimori.presentation.view.common.adapter.TagAdapter
import com.gnoemes.shikimori.presentation.view.common.fragment.EditRateFragment
import com.gnoemes.shikimori.presentation.view.common.fragment.LinkDialogFragment
import com.gnoemes.shikimori.presentation.view.common.fragment.StatisticDialogFragment
import com.gnoemes.shikimori.presentation.view.common.holders.*
import com.gnoemes.shikimori.presentation.view.rates.status.RateStatusDialog
import com.gnoemes.shikimori.utils.*
import com.gnoemes.shikimori.utils.images.ImageLoader
import kotlinx.android.synthetic.main.fragment_details.*
import kotlinx.android.synthetic.main.layout_details_content_with_search.view.*
import javax.inject.Inject

abstract class BaseDetailsFragment<Presenter : BaseDetailsPresenter<View, *>, View : BaseDetailsView> : BaseFragment<Presenter, View>(),
        BaseDetailsView, EditRateFragment.RateDialogCallback, RateStatusDialog.RateStatusCallback, LinkDialogFragment.LinkCallback {

    @Inject
    lateinit var imageLoader: ImageLoader

    private var detailsName: String? = null

    //the blurred poster's height from the layout, its least
    private var backgroundHeight = 0
    private var toolbarShown = false

    //the title on the toolbar's own background once the head has scrolled up under the toolbar, as when
    //the collapsing header the head was part of had collapsed (P7-T5 13.1)
    private fun updateToolbar() {
        val shown = nestedScroll.scrollY >= headContainer.height - toolbar.height
        if (shown == toolbarShown) return
        toolbarShown = shown
        if (shown) showToolbar() else hideToolbar()
    }

    //run after the layout pass: the page restores its scroll position while the screen is laid out, and
    //a title added to the toolbar then is never measured and does not show (P7-T5 Test 96g)
    private val toolbarCheck = Runnable { if (view != null) updateToolbar() }

    //the blurred poster reaches the bottom of a head taller than it, so no edge shows across a long
    //name (BUGS #80); and the head's height decides the toolbar too
    private fun fitHead() {
        val height = maxOf(backgroundHeight, headContainer.height)
        if (backgroundImage.layoutParams.height != height) backgroundImage.updateLayoutParams { this.height = height }
        updateToolbar()
    }

    protected open fun showToolbar() {
        toolbar?.apply {
            detailsName?.let { title = it } ?: setTitle(titleRes)
            background = ColorDrawable(context.colorAttr(R.attr.colorPrimary))
        }
    }

    protected open fun hideToolbar() {
        toolbar?.apply {
            title = null
            background = ColorDrawable(Color.TRANSPARENT)
        }
    }

    protected open lateinit var headHolder: DetailsHeadViewHolder
    protected open lateinit var infoHolder: DetailsInfoViewHolder
    protected open lateinit var actionHolder: DetailsActionViewHolder
    protected open lateinit var descriptionHolder: DetailsDescriptionViewHolder

    protected open val contentHolders = HashMap<DetailsContentType, DetailsContentViewHolder>()

    protected open val tagAdapter by lazy { TagAdapter(getPresenter()::onAction) }
    protected open val infoAdapter by lazy { InfoAdapter(getPresenter()::onContentClicked, imageLoader) }
    protected open val actionAdapter by lazy { ActionAdapter(getPresenter()::onAction) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View? {
        return inflater.inflate(getFragmentLayout(), container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar?.apply {
            addBackButton { getPresenter().onBackPressed() }
            title = null
            inflateMenu(menuRes)
            onMenuClick {
                when (it?.itemId) {
                    R.id.item_rate -> getPresenter().onAction(DetailsAction.RateStatusDialog)
                    R.id.item_web -> getPresenter().onAction(DetailsAction.OpenInBrowser)
                    R.id.item_share -> getPresenter().onAction(DetailsAction.Share)
                }
                true
            }
        }

        toolbarShown = false
        backgroundHeight = backgroundImage.layoutParams.height
        nestedScroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, _, _, _ ->
            v.removeCallbacks(toolbarCheck)
            v.post(toolbarCheck)
        })
        //after the layout that changed the head: the poster's height is set outside a layout pass
        headContainer.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if (bottom - top != oldBottom - oldTop) headContainer.post { if (this@BaseDetailsFragment.view != null) fitHead() }
        }

        headHolder = DetailsHeadViewHolder(headLayout, imageLoader, getPresenter()::onAction)
        infoHolder = DetailsInfoViewHolder(infoLayout, tagAdapter, infoAdapter)
        actionHolder = DetailsActionViewHolder(actionLayout, actionAdapter)
        descriptionHolder = DetailsDescriptionViewHolder(descriptionLayout, getPresenter()::onContentClicked)

        with(charactersLayout.searchView) {
            findViewById<LinearLayout>(R.id.search_bar)?.layoutTransition = null
            setOnCloseListener {
                getPresenter().onCharacterSearch(null)
                true
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    hideSoftInput()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    getPresenter().onCharacterSearch(newText)
                    return true
                }
            })
            applyAppStyle(startPaddingDp = 16, closeButton = false)
        }

        with(charactersLayout) {
            searchBtn.onClick {
                searchView.isIconified = false
                searchView.post {
                    TransitionManager.beginDelayedTransition(this@with as ViewGroup, Fade())
                    contentLabelView.gone()
                    searchBtn.gone()
                    searchView.visible()
                    closeBtn.visible()
                }
            }
            closeBtn.onClick {
                TransitionManager.beginDelayedTransition(this@with as ViewGroup, Fade())
                contentLabelView.visible()
                searchBtn.visible()
                searchView.setQuery("", false)
                searchView.gone()
                closeBtn.gone()
                searchView.isIconified = true
            }
        }
    }

    override fun onUpdateRate(rate: UserRate) {
        getPresenter().onUpdateOrCreateRate(rate)
    }

    override fun onDeleteRate(id: Long) {
        getPresenter().onDeleteRate(id)
    }

    override fun onStatusChanged(id: Long, newStatus: RateStatus) {
        getPresenter().onChangeRateStatus(newStatus)
    }

    override fun onLinkAction(action: DetailsAction.Link) {
        getPresenter().onAction(action)
    }

    /**
     * A text selection left open - the name and the second name can be selected - is brought back into
     * view when the app returns, which since the head scrolls with the page (13.1) moved the page up
     * to it from wherever it was (P7-T5 Q161). The selection ends when the page leaves the screen.
     */
    override fun onStop() {
        (view?.findFocus() as? TextView)?.let {
            (it.text as? Spannable)?.let(Selection::removeSelection)
            it.clearFocus()
        }
        super.onStop()
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    override fun getFragmentLayout(): Int = R.layout.fragment_details

    abstract val titleRes: Int

    /** The toolbar menu: status, open in the browser, share. */
    abstract val menuRes: Int

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun showRateDialog(title: String, targetId: Long, type: Type) {
        val dialog = EditRateFragment.newInstance(title, targetId, type)
        dialog.show(childFragmentManager, "RateTag")
    }

    override fun showRateMessage(message: String, rateId: Long) =
            coordinator.showUndoMessage(message) { getPresenter().onDeleteCanceled(rateId) }

    override fun setHeadItem(item: DetailsHeadItem) {
        detailsName = item.name
        headHolder.bind(item)

        if (!backgroundImage.hasImage()) {
            imageLoader.setBlurredImage(backgroundImage, item.image.original, sampling = 2)
        }
    }

    override fun setInfoItem(item: DetailsInfoItem) {
        infoHolder.bind(item)
    }

    override fun setDescriptionItem(item: DetailsDescriptionItem) {
        descriptionHolder.bind(item)
    }

    override fun setActionItem(item: DetailsActionItem) {
        actionHolder.bind(item)
    }

    override fun setContentItem(type: DetailsContentType, item: DetailsContentItem) {
        contentHolders[type]?.bind(type, item)
    }

    override fun showLinks(it: List<Link>) {
        val dialog = LinkDialogFragment.newInstance(it)
        dialog.show(childFragmentManager, "LinksTag")
    }

    override fun showStatusDialog(id: Long, name: String, currentStatus: RateStatus?, isAnime: Boolean) {
        hideSoftInput()
        val dialog = RateStatusDialog.newInstance(id, name, currentStatus, isAnime)
        dialog.show(childFragmentManager, "StatusDialog")
    }

    override fun showStatistic(title: String, scores: List<UserStatisticItem>, rates: List<UserStatisticItem>) {
        hideSoftInput()
        val dialog = StatisticDialogFragment.newInstance(title, scores, rates)
        dialog.show(childFragmentManager, "StatisticDialog")
    }
}
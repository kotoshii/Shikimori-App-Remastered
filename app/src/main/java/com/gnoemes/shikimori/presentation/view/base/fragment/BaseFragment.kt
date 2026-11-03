package com.gnoemes.shikimori.presentation.view.base.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.viewbinding.ViewBinding
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.LayoutDefaultListBinding
import com.gnoemes.shikimori.presentation.presenter.base.BasePresenter
import com.gnoemes.shikimori.presentation.view.base.activity.BaseNetworkView
import com.gnoemes.shikimori.presentation.view.common.widget.EmptyContentView
import com.gnoemes.shikimori.presentation.view.common.widget.NetworkErrorView
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.hideSoftInput
import com.gnoemes.shikimori.utils.visible
import com.gnoemes.shikimori.utils.visibleIf
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import javax.inject.Provider
import kotlin.properties.ReadOnlyProperty

abstract class BaseFragment<Presenter : BasePresenter<View>, View : BaseNetworkView>
    : MvpFragment(), BaseFragmentView {

    @Inject
    lateinit var presenterProvider: Provider<Presenter>

    private val viewHandler = Handler()

    private val bindings = ViewBindings { contentRoot }

    //false for a screen that overrides onCreateView and inflates its own layout without fragment_base
    private var inBaseLayout = false

    /**
     * The root of the screen's own layout: inside fragment_content ([layoutRoot]), or the whole view of a
     * screen that does without fragment_base (whose view can hold other screens' fragment_content: the pages
     * of ShikimoriMainFragment's pager).
     */
    private val contentRoot: android.view.View?
        get() {
            val view = view ?: return null
            if (!inBaseLayout) return view
            if (getFragmentLayout() == android.view.View.NO_ID) return null
            return context?.layoutRoot(view.findViewById(R.id.fragment_content), getFragmentLayout())
        }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /*
     * The views this class drives - the title, the loading spinner, the error and empty views, the content -
     * found by id in the screen's view, as the synthetics did. They sit in fragment_base for most screens, in
     * the screen's own layout for the others, or nowhere, so no one binding class has them. The first view
     * with the id wins: on the character and person pages the spinner is their first section's, as before
     * (P7-T14 Q2).
     */
    protected val screenToolbar: Toolbar? get() = screenView(R.id.toolbar)
    protected val screenProgress: android.view.View? get() = screenView(R.id.progressBar)
    protected val screenErrorView: NetworkErrorView? get() = screenView(R.id.networkErrorView)
    protected val screenEmptyView: EmptyContentView? get() = screenView(R.id.emptyContentView)
    private val screenContent: android.view.View? get() = screenView(R.id.fragment_content)

    //each found once per view and kept, as the synthetics' cache kept it: list rows carry the same ids
    //(item_page_error's networkErrorView, item_progress's progressBar) and sit earlier in the tree than
    //fragment_base's own, so a later search could find a row's (P7-T14, the 5a review)
    private val screenViews = HashMap<Int, android.view.View>()

    /** The first view with [id] in the screen's view, found once and kept until the view goes. */
    protected fun <T : android.view.View> screenView(id: Int): T? {
        @Suppress("UNCHECKED_CAST")
        (screenViews[id] as T?)?.let { return it }
        return view?.findViewById<T>(id)?.also { screenViews[id] = it }
    }

    /** layout_default_list, the shared list most list screens include (without an id): bound from its root. */
    protected val defaultList: LayoutDefaultListBinding
        get() = LayoutDefaultListBinding.bind(requireNotNull(screenView(R.id.refreshLayout)))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): android.view.View? {
        inBaseLayout = true
        val view = inflater.inflate(R.layout.fragment_base, container, false)
        if (getFragmentLayout() != android.view.View.NO_ID) {
            inflater.inflate(getFragmentLayout(), view.findViewById(R.id.fragment_content), true)
        }
        return view
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //every screen's error view offers «Повторить»: 11 screens showed the text alone, their list
        //hidden, so not even a pull could try again (P7-T5 Q167)
        screenErrorView?.apply {
            setText(R.string.common_error_message_without_pull)
            callback = { getPresenter().onRetry() }
            showButton()
            gone()
        }
        screenEmptyView?.gone()
    }

    override fun onDestroyView() {
        hideSoftInput()
        viewHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        bindings.clear()
        screenViews.clear()
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    protected abstract fun getPresenter(): Presenter

    @LayoutRes
    protected abstract fun getFragmentLayout(): Int

    /**
     * A binding of the screen's own layout: `private val binding by viewBinding(FragmentXBinding::bind)`.
     * Bound on first use, dropped when the view goes ([ViewBindings]).
     */
    protected fun <T : ViewBinding> viewBinding(bind: (android.view.View) -> T): ReadOnlyProperty<Any, T> =
            bindings.of(bind)

    ///////////////////////////////////////////////////////////////////////////
    // UI METHODS
    ///////////////////////////////////////////////////////////////////////////

    protected fun postViewAction(action: () -> Unit) {
        viewHandler.post { action.invoke() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                getPresenter().onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    ///////////////////////////////////////////////////////////////////////////
    // MVP
    ///////////////////////////////////////////////////////////////////////////

    override fun onBackPressed() = getPresenter().onBackPressed()

    override fun setTitle(title: String) {
        screenToolbar?.title = title
    }

    override fun setTitle(stringRes: Int) {
        screenToolbar?.setTitle(stringRes)
    }

    override fun showContent(show: Boolean) {
        screenContent?.visibleIf { show }
    }

    override fun onShowLoading() {
        screenProgress?.visible()
    }

    override fun onHideLoading() {
        screenProgress?.gone()
    }

    override fun onShowLightLoading() {
        screenProgress?.visible()
    }

    override fun onHideLightLoading() {
        screenProgress?.gone()
    }

    /**
     * The page the error view stands in for, on a screen whose loading look is a shimmer inside the page
     * and whose [showContent] is therefore off (the title, character and person pages): hidden while the
     * error view shows, back when a new load starts. The error view lay over the shimmer (P7-T5 Q166,
     * Test 104d).
     */
    protected open val pageView: android.view.View? get() = null

    override fun showNetworkView() {
        screenErrorView?.visible()
        pageView?.gone()
    }

    override fun showErrorMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun hideNetworkView() {
        screenErrorView?.gone()
        pageView?.visible()
    }

    override fun showEmptyView() {
        screenEmptyView?.visible()
    }

    override fun hideEmptyView() {
        screenEmptyView?.gone()
    }
}
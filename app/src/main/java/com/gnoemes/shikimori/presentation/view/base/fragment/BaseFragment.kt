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
import androidx.viewbinding.ViewBinding
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.presentation.presenter.base.BasePresenter
import com.gnoemes.shikimori.presentation.view.base.activity.BaseNetworkView
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.hideSoftInput
import com.gnoemes.shikimori.utils.visible
import com.gnoemes.shikimori.utils.visibleIf
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_base.*
import kotlinx.android.synthetic.main.layout_default_placeholders.*
import kotlinx.android.synthetic.main.layout_progress.*
import kotlinx.android.synthetic.main.layout_toolbar.*
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
        networkErrorView?.apply {
            setText(R.string.common_error_message_without_pull)
            callback = { getPresenter().onRetry() }
            showButton()
            gone()
        }
        emptyContentView?.gone()
    }

    override fun onDestroyView() {
        hideSoftInput()
        viewHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
        bindings.clear()
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
        toolbar?.title = title
    }

    override fun setTitle(stringRes: Int) {
        toolbar?.setTitle(stringRes)
    }

    override fun showContent(show: Boolean) {
        fragment_content?.visibleIf { show }
    }

    override fun onShowLoading() {
        progressBar?.visible()
    }

    override fun onHideLoading() {
        progressBar?.gone()
    }

    override fun onShowLightLoading() {
        progressBar?.visible()
    }

    override fun onHideLightLoading() {
        progressBar?.gone()
    }

    /**
     * The page the error view stands in for, on a screen whose loading look is a shimmer inside the page
     * and whose [showContent] is therefore off (the title, character and person pages): hidden while the
     * error view shows, back when a new load starts. The error view lay over the shimmer (P7-T5 Q166,
     * Test 104d).
     */
    protected open val pageView: android.view.View? get() = null

    override fun showNetworkView() {
        networkErrorView?.visible()
        pageView?.gone()
    }

    override fun showErrorMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun hideNetworkView() {
        networkErrorView?.gone()
        pageView?.visible()
    }

    override fun showEmptyView() {
        emptyContentView?.visible()
    }

    override fun hideEmptyView() {
        emptyContentView?.gone()
    }
}
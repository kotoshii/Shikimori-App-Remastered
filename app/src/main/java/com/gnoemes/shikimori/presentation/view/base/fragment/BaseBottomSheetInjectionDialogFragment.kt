package com.gnoemes.shikimori.presentation.view.base.fragment

import android.content.Context
import android.widget.Toast
import com.gnoemes.shikimori.presentation.presenter.base.BasePresenter
import com.gnoemes.shikimori.presentation.view.base.activity.BaseNetworkView
import com.gnoemes.shikimori.presentation.view.common.widget.NetworkErrorView
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.visible
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import javax.inject.Provider

abstract class BaseBottomSheetInjectionDialogFragment<Presenter : BasePresenter<View>, View : BaseNetworkView> : BaseBottomSheetDialogFragment(), BaseFragmentView {

    @Inject
    lateinit var presenterProvider: Provider<Presenter>

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    abstract val presenter : Presenter

    override fun onBackPressed() {
        dismiss()
    }

    override fun showErrorMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /** The sheet's error view, for a sheet whose layout has one (layout_default_placeholders): «Эпизоды». */
    protected open val networkErrorView: NetworkErrorView? get() = null

    //the error view of a sheet that has one, as BaseFragment does for screens. Without these BaseView's
    //empty defaults ran, and a failed load left the «Эпизоды» sheet blank, with no «Повторить»
    override fun showNetworkView() {
        networkErrorView?.visible()
    }

    override fun hideNetworkView() {
        networkErrorView?.gone()
    }

    override fun setTitle(title: String) = Unit
    override fun setTitle(stringRes: Int) = Unit
}
package com.gnoemes.shikimori.presentation.view.base.fragment

import android.content.Context
import android.widget.Toast
import com.gnoemes.shikimori.presentation.presenter.base.BasePresenter
import com.gnoemes.shikimori.presentation.view.base.activity.BaseNetworkView
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.visible
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.layout_default_placeholders.*
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

    //the error view of a sheet that has one (layout_default_placeholders), as BaseFragment does for
    //screens. Without these BaseView's empty defaults ran, and a failed load left the «Эпизоды» sheet
    //blank, with no «Повторить»
    override fun showNetworkView() {
        networkErrorView?.visible()
    }

    override fun hideNetworkView() {
        networkErrorView?.gone()
    }

    override fun setTitle(title: String) = Unit
    override fun setTitle(stringRes: Int) = Unit
}
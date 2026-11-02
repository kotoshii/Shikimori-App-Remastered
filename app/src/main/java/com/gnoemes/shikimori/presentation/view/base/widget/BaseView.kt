package com.gnoemes.shikimori.presentation.view.base.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding

abstract class BaseView<B : ViewBinding> @JvmOverloads constructor(context: Context,
                                                                  attrs: AttributeSet? = null,
                                                                  defStyleInt: Int = 0
) : FrameLayout(context, attrs, defStyleInt) {

    /**
     * The view's layout, inflated into it. Set here, from the constructor, because [init] runs from it too:
     * a property of the subclass is not set yet when its `init` override runs (P7-T14).
     */
    protected lateinit var binding: B
        private set

    init {
        init(context, attrs)
    }

    protected open fun init(context: Context, attrs: AttributeSet? = null) {
        binding = inflate(LayoutInflater.from(context), this)
    }

    /** The view's layout inflated into [parent]: `ViewXBinding.inflate(inflater, parent, true)`. */
    protected abstract fun inflate(inflater: LayoutInflater, parent: ViewGroup): B
}

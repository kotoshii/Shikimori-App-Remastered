package com.gnoemes.shikimori.presentation.view.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ViewNetworkErrorBinding
import com.gnoemes.shikimori.presentation.view.base.widget.BaseView
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.visible

class NetworkErrorView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyleInt: Int = 0
) : BaseView<ViewNetworkErrorBinding>(context, attrs, defStyleInt) {

    lateinit var callback: (View) -> Unit

    override fun inflate(inflater: LayoutInflater, parent: ViewGroup) = ViewNetworkErrorBinding.inflate(inflater, parent, true)

    override fun init(context: Context, attrs: AttributeSet?) {
        super.init(context, attrs)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.NetworkErrorView)
        binding.descriptionTextView.text = ta.getText(R.styleable.NetworkErrorView_text)
        ta.recycle()

        binding.btnView.setOnClickListener {
            if (::callback.isInitialized) {
                callback.invoke(it)
            }
        }

    }

    fun setText(@StringRes stringRes: Int) {
        binding.descriptionTextView.setText(stringRes)
    }

    fun showButton() {
        binding.btnView.visible()
    }

    //for a row inside a list, where the big picture would take half the screen
    fun hideImage() {
        binding.imageView.gone()
    }
}
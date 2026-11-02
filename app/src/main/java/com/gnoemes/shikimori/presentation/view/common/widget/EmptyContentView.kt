package com.gnoemes.shikimori.presentation.view.common.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import com.gnoemes.shikimori.databinding.ViewEmptyBinding
import com.gnoemes.shikimori.presentation.view.base.widget.BaseView

class EmptyContentView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyleInt: Int = 0
) : BaseView<ViewEmptyBinding>(context, attrs, defStyleInt) {

    override fun inflate(inflater: LayoutInflater, parent: ViewGroup) = ViewEmptyBinding.inflate(inflater, parent, true)

    fun setText(@StringRes textRes: Int) {
        binding.descriptionTextView.setText(textRes)
    }
}
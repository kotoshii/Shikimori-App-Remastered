package com.gnoemes.shikimori.presentation.view.common.widget

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.presentation.view.base.widget.BaseView
import kotlinx.android.synthetic.main.view_empty.view.*

class EmptyContentView @JvmOverloads constructor(context: Context,
                                                 attrs: AttributeSet? = null,
                                                 defStyleInt: Int = 0
) : BaseView(context, attrs, defStyleInt) {

    override fun getLayout(): Int = R.layout.view_empty

    fun setText(text: String) {
        descriptionTextView.text = text
    }

    fun setText(@StringRes textRes: Int) {
        descriptionTextView.setText(textRes)
    }
}
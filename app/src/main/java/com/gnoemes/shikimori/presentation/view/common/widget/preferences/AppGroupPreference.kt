package com.gnoemes.shikimori.presentation.view.common.widget.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.utils.drawable
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.onClick
import kotlinx.android.synthetic.main.view_app_info_group.view.*

class AppGroupPreference @JvmOverloads constructor(context: Context,
                                                   attrs: AttributeSet? = null,
                                                   defStyleInt: Int = 0
) : PreferenceGroup(context, attrs, defStyleInt) {

    init {
        layoutResource = R.layout.view_app_info_group
    }

    var feedbackClickListener: View.OnClickListener? = null
    var forumClickListener: View.OnClickListener? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        with(holder.itemView) {
            sendLayout.icon()?.setImageDrawable(context.drawable(R.drawable.icon_github_setting))
            sendLayout.title()?.text = context.getString(R.string.settings_about_send_title)
            sendLayout.summary()?.gone()
            sendLayout.onClick { feedbackClickListener?.onClick(it) }

            fourPdaLayout.icon()?.setImageDrawable(context.drawable(R.drawable.icon_4pda_setting))
            fourPdaLayout.title()?.text = context.getString(R.string.settings_about_forum_title)
            fourPdaLayout.summary()?.gone()
            fourPdaLayout.onClick { forumClickListener?.onClick(it) }
        }
    }

    private fun View.icon() : ImageView? = findViewById(android.R.id.icon)
    private fun View.title() : TextView? = findViewById(android.R.id.title)
    private fun View.summary() : TextView? = findViewById(android.R.id.summary)
}
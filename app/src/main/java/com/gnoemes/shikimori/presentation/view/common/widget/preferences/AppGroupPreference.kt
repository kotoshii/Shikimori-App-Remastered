package com.gnoemes.shikimori.presentation.view.common.widget.preferences

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.databinding.ViewAppInfoGroupBinding
import com.gnoemes.shikimori.utils.drawable
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.onClick

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
        //the row's own context, as before: inside with(binding) the name would be the preference's
        val context = holder.itemView.context
        with(ViewAppInfoGroupBinding.bind(holder.itemView)) {
            sendLayout.root.icon()?.setImageDrawable(context.drawable(R.drawable.icon_github_setting))
            sendLayout.root.title()?.text = context.getString(R.string.settings_about_send_title)
            sendLayout.root.summary()?.gone()
            sendLayout.root.onClick { feedbackClickListener?.onClick(it) }

            fourPdaLayout.root.icon()?.setImageDrawable(context.drawable(R.drawable.icon_4pda_setting))
            fourPdaLayout.root.title()?.text = context.getString(R.string.settings_about_forum_title)
            fourPdaLayout.root.summary()?.gone()
            fourPdaLayout.root.onClick { forumClickListener?.onClick(it) }
        }
    }

    private fun View.icon() : ImageView? = findViewById(android.R.id.icon)
    private fun View.title() : TextView? = findViewById(android.R.id.title)
    private fun View.summary() : TextView? = findViewById(android.R.id.summary)
}
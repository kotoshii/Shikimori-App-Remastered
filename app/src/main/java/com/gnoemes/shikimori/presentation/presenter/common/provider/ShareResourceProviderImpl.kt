package com.gnoemes.shikimori.presentation.presenter.common.provider

import android.content.Context
import com.gnoemes.shikimori.R
import javax.inject.Inject

class ShareResourceProviderImpl @Inject constructor(
        private val context: Context
) : ShareResourceProvider {

    override fun getEpisodeShareFormattedMessage(title: String, episode: Int, url: String, details: List<String>): String {
        //same separator as the header above it, so the two lines read as one block
        val detailsLine = if (details.isEmpty()) "" else "\n" + details.joinToString(SEPARATOR)

        return "[" +
                String.format(context.getString(R.string.episode_number), episode) +
                SEPARATOR +
                context.getString(R.string.app_name) +
                "]" +
                "\n" +
                title +
                detailsLine +
                "\n\n" +
                url
    }

    companion object {
        private const val SEPARATOR = " • "
    }
}

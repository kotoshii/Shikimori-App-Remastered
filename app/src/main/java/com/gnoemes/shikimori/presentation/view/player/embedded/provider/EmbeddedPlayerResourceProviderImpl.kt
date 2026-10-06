package com.gnoemes.shikimori.presentation.view.player.embedded.provider

import android.content.Context
import com.gnoemes.shikimori.R
import javax.inject.Inject

class EmbeddedPlayerResourceProviderImpl @Inject constructor(
        private val context: Context
) : EmbeddedPlayerResourceProvider {

    override val hostingChallengeMessage: String
        get() = context.getString(R.string.series_hosting_challenge)
    override val playerErrorMessage: String
        get() = context.getString(R.string.player_error)
    override val translationNotFound: String
        get() = context.getString(R.string.translation_not_found)
}
package com.gnoemes.shikimori.presentation.view.player.embedded.provider

interface EmbeddedPlayerResourceProvider  {
    val hostingErrorMessage : String
    val hostingChallengeMessage : String
    val playerErrorMessage : String
    val translationNotFound : String
}
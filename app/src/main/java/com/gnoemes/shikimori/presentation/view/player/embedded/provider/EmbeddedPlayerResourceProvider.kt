package com.gnoemes.shikimori.presentation.view.player.embedded.provider

interface EmbeddedPlayerResourceProvider  {
    val hostingChallengeMessage : String
    val playerErrorMessage : String
    val videoNotFoundMessage : String
    val translationNotFound : String
}
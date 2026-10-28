package com.gnoemes.shikimori.entity.series.presentation

/**
 * The watch screen's ‹ «# N» ›. [isEnabled] is false while the episode list loads: they stay in their
 * places, greyed out, rather than going and coming back. [hasArrows] for a title with more than one
 * episode, or one shown past the first; an arrow that cannot be used is greyed out, not hidden.
 */
data class EpisodeControls(
        val episode: Int,
        val isEnabled: Boolean,
        val hasArrows: Boolean,
        val canGoBack: Boolean,
        val canGoForward: Boolean
)

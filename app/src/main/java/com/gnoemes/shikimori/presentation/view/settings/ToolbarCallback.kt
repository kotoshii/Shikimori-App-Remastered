package com.gnoemes.shikimori.presentation.view.settings

interface ToolbarCallback {
    fun showToolbarMenu()
    fun hideToolbarMenu()

    //a separate pair, because a screen may want the hint without the accept tick
    fun showToolbarHint()
    fun hideToolbarHint()
}
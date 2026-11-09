package com.gnoemes.shikimori.entity.common.presentation.richtext

/**
 * What a tap in user-written text asks for. Every screen's presenter answers it (`BaseNetworkPresenter`):
 * the content view hands the taps to its screen, which knows the router of its tab.
 */
interface RichTextListener {

    /** A link, a mention, a title's name, an avatar. */
    fun onLinkClicked(target: LinkTarget)

    /** An image a tap enlarges: [images] are all such images of the text (one gallery, as on the site). */
    fun onImageClicked(images: List<Image>, index: Int)
}

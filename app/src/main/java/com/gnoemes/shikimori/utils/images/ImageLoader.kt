package com.gnoemes.shikimori.utils.images

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView

interface ImageLoader {

    fun setCircleImage(image : ImageView, url : String?)

    fun setImageWithPlaceHolder(image: ImageView, url: String?)

    fun setImageListItem(image: ImageView, url: String?)

    fun setBlurredImage(image : ImageView, url : String?,  radius : Int = 25, sampling : Int = 1)

    fun setBlurredCircleImage(image : ImageView, url : String?, radius : Int = 25, sampling : Int = 1)

    fun clearImage(image: ImageView)

    /**
     * A picture drawn inside a line of user text (a smiley, an avatar), [size] pixels square at most. An
     * animated GIF comes as a drawable that animates once started. The load ends with [view]'s screen, or
     * with [ImageRequest.cancel].
     */
    fun loadTextPicture(view: View, url: String, size: Int, onReady: (Drawable) -> Unit): ImageRequest

    /**
     * An image in user text, never larger than [maxWidth]×[maxHeight] pixels and never enlarged; an
     * animated GIF animates. [onSize] gets the loaded picture's size in pixels, [onFailed] runs when it
     * could not load.
     */
    fun setTextImage(image: ImageView, url: String, maxWidth: Int, maxHeight: Int, onSize: (width: Int, height: Int) -> Unit, onFailed: () -> Unit)
}

/** A load that no view owns, stopped by hand. */
interface ImageRequest {
    fun cancel()
}

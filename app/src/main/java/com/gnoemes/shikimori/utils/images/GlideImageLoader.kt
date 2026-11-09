package com.gnoemes.shikimori.utils.images

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.gnoemes.shikimori.utils.images.blur.BlurTransformation
import javax.inject.Inject

class GlideImageLoader @Inject constructor(
        private val context: Context
) : ImageLoader {

    val glide = GlideApp.with(context)

    override fun setCircleImage(image: ImageView, url: String?) {
        val target = PictureTarget(image)
        GlideApp.with(image)
                .asBitmap()
                .load(url)
                .dontAnimate()
                .listener(target)
                .into(target)
    }

    override fun setImageWithPlaceHolder(image: ImageView, url: String?) {
        val target = PictureTarget(image)
        GlideApp.with(image)
                .asBitmap()
                .load(url)
                .listener(target)
                .centerCrop()
                .into(target)
    }

    override fun setImageListItem(image: ImageView, url: String?) {
        val target = PictureTarget(image).apply { waitForLayout() }
        GlideApp.with(image)
                .asBitmap()
                .dontAnimate()
                .centerCrop()
                .load(url)
                .override(image.measuredWidth / 2, image.measuredHeight / 2)
                .listener(target)
                .into(target)
    }

    override fun setBlurredImage(image: ImageView, url: String?, radius : Int, sampling : Int) {
        GlideApp.with(image)
                .asBitmap()
                .load(url)
                .transform(BlurTransformation(radius, sampling))
                .priority(Priority.HIGH)
                .dontAnimate()
                .into(image)
    }

    override fun setBlurredCircleImage(image: ImageView, url: String?, radius: Int, sampling: Int) {
        GlideApp.with(image)
                .asDrawable()
                .load(url)
                .transform(BlurTransformation(radius, sampling))
                .dontAnimate()
                .into(image)
    }

    override fun clearImage(image: ImageView) {
        glide.clear(image)
    }

    override fun loadTextPicture(view: View, url: String, size: Int, onReady: (Drawable) -> Unit): ImageRequest {
        val target = object : CustomTarget<Drawable>(size, size) {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                (resource as? GifDrawable)?.setLoopCount(GifDrawable.LOOP_FOREVER)
                onReady(resource)
            }

            override fun onLoadCleared(placeholder: Drawable?) = Unit
        }
        val requests = GlideApp.with(view)
        //no dontAnimate(): in Glide 4 it decodes a GIF as one still frame (GifOptions.DISABLE_ANIMATION)
        requests.load(url).into(target)
        return object : ImageRequest {
            override fun cancel() = requests.clear(target)
        }
    }

    //the loaded size is read from the picture itself: a drawable's intrinsic size can be scaled by density
    override fun setTextImage(image: ImageView, url: String, maxWidth: Int, maxHeight: Int, onSize: (width: Int, height: Int) -> Unit, onFailed: () -> Unit) {
        GlideApp.with(image)
                .load(url)
                .override(maxWidth, maxHeight)
                //smaller than asked stays as it is; the view scales it to the size it gave itself
                .downsample(DownsampleStrategy.CENTER_INSIDE)
                .dontTransform()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        onFailed()
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        when (resource) {
                            is BitmapDrawable -> onSize(resource.bitmap.width, resource.bitmap.height)
                            else -> onSize(resource.intrinsicWidth, resource.intrinsicHeight)
                        }
                        return false
                    }
                })
                .into(image)
    }
}
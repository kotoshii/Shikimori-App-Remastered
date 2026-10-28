package com.gnoemes.shikimori.utils.images

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Priority
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
}
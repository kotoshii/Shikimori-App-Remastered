package com.gnoemes.shikimori.utils.images

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.widget.ImageViewCompat
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.utils.colorAttr
import de.hdodenhof.circleimageview.CircleImageView
import java.net.HttpURLConnection.HTTP_NOT_FOUND

/**
 * Whether a load failed because the image is not there - the server answered 404, or there was no
 * address - rather than because it could not be fetched: the one rule for posters, round pictures and
 * the screenshot viewer.
 */
fun isNotFound(e: GlideException?, model: Any?): Boolean =
        model == null || e?.rootCauses?.any { (it as? HttpException)?.statusCode == HTTP_NOT_FOUND } == true

/**
 * A poster, a picture in a card or a round one (a character, an avatar). Pass it to the request as its
 * listener too, as with [TouchImageTargetView]: the listener learns why a load failed. Only an image
 * the server does not have gets the «404 not found» picture; any other failure, a lost connection
 * included, a small icon inside a thin outline. Every failed poster read «404 not found», and a round
 * picture stayed an empty circle (BUGS #77, P7-T5 Q138 a, Q168 a).
 */
class PictureTarget(view: ImageView) : BitmapImageViewTarget(view), RequestListener<Bitmap> {

    //a round view draws any picture cropped to its circle and allows no other scale type: its outline
    //is its own border, and the icon is padded out to its size to stay small
    private val round = view as? CircleImageView

    //the view's own look, noted on it by its first target: a failure changes it, and the next load
    //puts it back
    private val original = view.getTag(R.id.picture_original) as? Original
            ?: Original(view.scaleType, view.background, round?.borderWidth ?: 0, round?.borderColor ?: 0)
                    .also { view.setTag(R.id.picture_original, it) }

    private var notFound = false

    //the size the picture is loaded for: a round view's icon is padded out to it. Offline the load can
    //fail before the view is laid out, at width 0, and the icon then filled the circle (P7-T5 Test 104b)
    private var width = 0
    private var height = 0

    override fun getSize(cb: SizeReadyCallback) = super.getSize { width, height ->
        this.width = width
        this.height = height
        cb.onSizeReady(width, height)
    }

    //called before the target's own onLoadFailed below
    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
        notFound = isNotFound(e, model)
        return false
    }

    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean = false

    override fun onLoadStarted(placeholder: Drawable?) {
        restore()
        super.onLoadStarted(placeholder)
    }

    override fun onLoadCleared(placeholder: Drawable?) {
        restore()
        super.onLoadCleared(placeholder)
    }

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        restore()
        super.onResourceReady(resource, transition)
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        super.onLoadFailed(errorDrawable)
        when {
            //filling the card, as a poster would
            notFound -> {
                if (round == null) view.scaleType = ImageView.ScaleType.CENTER_CROP
                view.setImageResource(R.drawable.missing_original)
            }
            round != null -> {
                round.borderWidth = outlineWidth()
                round.borderColor = outlineColor()
                tintIcon()
                round.setImageDrawable(icon().let {
                    val x = inset(width.takeIf { w -> w > 0 } ?: round.width, it.intrinsicWidth)
                    val y = inset(height.takeIf { h -> h > 0 } ?: round.height, it.intrinsicHeight)
                    InsetDrawable(it, x, y, x, y)
                })
            }
            //small, in the middle, inside an outline: alone it blended into the card, and on a title
            //page into the blurred poster behind (P7-T5 Test 102)
            else -> {
                view.scaleType = ImageView.ScaleType.CENTER
                tintIcon()
                view.setImageDrawable(icon())
                view.background = outline()
            }
        }
    }

    private fun icon(): Drawable = AppCompatResources.getDrawable(view.context, R.drawable.ic_image_unavailable)!!

    //through the view, before the icon is set: the view's own tint, cleared for the next load, also
    //cleared a tint put on the icon itself, which then drew black; and a round view turns the icon
    //into its bitmap as it is set
    private fun tintIcon() =
            ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(view.context.colorAttr(R.attr.colorOnPrimarySecondary)))

    //the line of the list's cards, so a picture missing from a card reads as part of it
    private fun outlineColor() = view.context.colorAttr(R.attr.colorDivider)

    private fun outlineWidth() = view.resources.getDimensionPixelSize(R.dimen.picture_outline_width)

    private fun inset(size: Int, iconSize: Int) = ((size - iconSize) / 2).coerceAtLeast(0)

    //the shape of the card the picture sits in, so the line follows the card's edge where the card
    //cuts the picture
    private fun outline() = GradientDrawable().apply {
        setStroke(outlineWidth(), outlineColor())
        cornerRadius = generateSequence(view.parent) { it.parent }.filterIsInstance<CardView>().firstOrNull()?.radius ?: 0f
    }

    private fun restore() {
        view.background = original.background
        ImageViewCompat.setImageTintList(view, null)
        if (round != null) {
            round.borderWidth = original.borderWidth
            round.borderColor = original.borderColor
        } else view.scaleType = original.scaleType
    }

    private class Original(val scaleType: ImageView.ScaleType, val background: Drawable?, val borderWidth: Int, val borderColor: Int)
}

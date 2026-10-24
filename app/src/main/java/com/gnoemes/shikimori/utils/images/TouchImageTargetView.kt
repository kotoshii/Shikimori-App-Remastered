package com.gnoemes.shikimori.utils.images

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ProgressBar
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.HttpException
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.utils.gone
import com.gnoemes.shikimori.utils.visible
import com.gnoemes.shikimori.presentation.view.common.widget.TouchImageView
import java.net.HttpURLConnection.HTTP_NOT_FOUND

/**
 * A screenshot in the viewer. Pass it to the request as its listener too: the listener learns why a
 * load failed, which the target alone is not told. Only a file the server does not have gets the
 * «404 not found» picture; any other failure, a lost connection included, shows [errorView]
 * (P7-T5 Q98). Glide restarts a failed load by itself when the connection is back, so every start
 * and every success hides [errorView] again: only the «Повторить» tap did before.
 */
class TouchImageTargetView(
        view: TouchImageView,
        private val progress: ProgressBar? = null,
        private val errorView: View? = null
) : CustomViewTarget<TouchImageView, Bitmap>(view), RequestListener<Bitmap> {

    private var notFound = false

    //called before the target's own onLoadFailed below
    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
        notFound = model == null || e?.rootCauses?.any { (it as? HttpException)?.statusCode == HTTP_NOT_FOUND } == true
        return false
    }

    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean = false

    //the spinner goes too: it kept turning over the "missing" picture (T5-33)
    override fun onLoadFailed(errorDrawable: Drawable?) {
        progress?.gone()
        val errorView = errorView
        if (notFound || errorView == null) view.setImageResource(R.drawable.missing_original)
        else errorView.visible()
    }

    override fun onResourceCleared(placeholder: Drawable?) {
    }

    override fun onResourceLoading(placeholder: Drawable?) {
        errorView?.gone()
        progress?.visible()
    }

    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
        errorView?.gone()
        progress?.gone()
        view.setImageBitmap(resource)
    }
}

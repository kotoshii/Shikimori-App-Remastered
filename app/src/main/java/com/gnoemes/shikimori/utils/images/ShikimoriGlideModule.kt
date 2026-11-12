package com.gnoemes.shikimori.utils.images

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.gnoemes.shikimori.App
import okhttp3.Cache
import java.io.File
import java.io.InputStream

@GlideModule
class ShikimoriGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        with(builder) {
            setDiskCache(InternalCacheDiskCacheFactory(context, (50L * 1024 * 1024)))
            setDefaultRequestOptions(RequestOptions().format(DecodeFormat.PREFER_RGB_565))
            setDefaultTransitionOptions(Bitmap::class.java, BitmapTransitionOptions.withCrossFade())
            setLogLevel(Log.DEBUG)
            //the memory cache and the bitmap pool are Glide's own, sized by the device (MemorySizeCalculator:
            //about two screens of pixels, less on a low-memory phone). They were 5 MB each: about 16 list
            //posters, so one scrolled a few rows away was decoded again from the disk when it came back, its
            //place empty meanwhile (P10-T3 Q13; seen in Glide's log)
        }
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        val client = (context.applicationContext as App).httpClient.newBuilder()
                .cache(Cache(File(context.cacheDir, "net_cache"), 5L * 1024 * 1024))
                .build()
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(client))
    }
}
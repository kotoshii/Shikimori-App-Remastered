package com.gnoemes.shikimori.presentation.presenter.series.translations.converter

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.entity.series.presentation.TranslationViewModel
import com.gnoemes.shikimori.utils.Utils
import com.gnoemes.shikimori.utils.color
import com.gnoemes.shikimori.utils.colorSpan
import java.net.URLEncoder
import javax.inject.Inject

class TranslationsViewModelConverterImpl @Inject constructor(
        private val context: Context
) : TranslationsViewModelConverter {

    private val unknownAuthor by lazy { context.getString(R.string.translation_unknown_author) }
    //TODO fix attribute color (crashes)
    private val descriptionColor by lazy { context.color(R.color.default_colorOnPrimarySecondary) }

    override fun convertTranslations(translations: List<Translation>, setting: TranslationSetting?): List<TranslationViewModel> {


        return translations
                .mapIndexed { index, translation -> if (translation.author.isEmpty()) translation.copy(author = "$unknownAuthor $index") else translation }
                .groupBy { it.author.trim() }
                .mapNotNull { convertTranslation(it, setting) }
    }


    private fun convertTranslation(it: Map.Entry<String, List<Translation>>, setting: TranslationSetting?): TranslationViewModel? {
        if (it.value.isEmpty()) return null

        val author = if (it.key.contains(unknownAuthor)) it.key.replace(Regex("\\d"), "") else it.key
        val builder = SpannableStringBuilder()

        fun appendDot(withSpace: Boolean = false) {
            val space = "  "
            val dot = "\t•"
            builder.append(space.plus(dot).colorSpan(descriptionColor))
            if (withSpace) builder.append(space)
        }

        val hasBd = it.value.find { it.quality == TranslationQuality.BD || it.quality == TranslationQuality.DVD } != null
        val isSameAuthor = author == setting?.lastAuthor
        val canBeDownloaded = it.value.find { Utils.isHostingSupports(it.hosting, true) } != null


        if (isSameAuthor) builder.append("*").setSpan(ImageSpan(context, R.drawable.ic_translation_watched), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (hasBd) {
            if (builder.isNotEmpty()) appendDot(true)
            val blueRay = context.getString(R.string.translation_blue_ray).colorSpan(descriptionColor)
            builder.append(blueRay)
        }
        if (canBeDownloaded) {
            if (builder.isNotEmpty()) appendDot(true)
            val text = context.getString(R.string.translation_downloadable).colorSpan(descriptionColor)
            builder.append(text)
        }

        val description: CharSequence? = if (builder.isNotEmpty()) builder else null
        val videos = it.value.map { t -> convertVideo(t) }

        return TranslationViewModel(
                it.value.first().videoId,
                it.value.first().type,
                author,
                description,
                videos,
                isSameAuthor,
                it.value.first().episodesSize
        )
    }

    override fun convertTrack(hosting: VideoHosting, track: Track): Pair<String, String> {
        var title = hosting.synonymType
        //TODO colorspan for quality
        if (track.quality != "unknown") title += "   ".plus("${track.quality}p")
        return Pair(title, track.url)
    }

    private fun convertVideo(t: Translation): TranslationVideo {

        val simpleAuthor = URLEncoder.encode(t.author.replace(Regex("\\(.+\\)"), "").trim(), "utf-8")

        return TranslationVideo(
                t.videoId,
                t.animeId,
                t.episodeId,
                //TODO multi language
                "russian",
                t.author,
                simpleAuthor,
                t.type,
                t.hosting
        )
    }

}
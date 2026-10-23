package com.gnoemes.shikimori.presentation.presenter.series.translations.converter

import android.content.Context
import com.gnoemes.shikimori.R
import com.gnoemes.shikimori.data.repository.series.shikimori.parser.HostingParsers
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.SeriesDownloadItem
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.entity.series.presentation.TranslationViewModel
import com.gnoemes.shikimori.utils.labelRes
import com.gnoemes.shikimori.utils.toUri
import javax.inject.Inject

class TranslationsViewModelConverterImpl @Inject constructor(
        private val context: Context,
        private val parsers: HostingParsers
) : TranslationsViewModelConverter {

    private val unknownAuthor by lazy { context.getString(R.string.translation_unknown_author) }

    //by author. A translation with no author stays alone, under its position: nothing says two of
    //them are by the same people
    override fun convertTranslations(translations: List<Translation>, setting: TranslationSetting?): List<TranslationViewModel> =
            translations.withIndex()
                    .groupBy({ (index, translation) -> translation.author.trim().let { Pair(it, if (it.isEmpty()) index else -1) } }, { it.value })
                    .mapNotNull { (key, group) -> convertTranslation(key.first, group, setting) }

    //an empty author is an unknown one; the label says so only here, in the view model
    private fun convertTranslation(author: String, translations: List<Translation>, setting: TranslationSetting?): TranslationViewModel? {
        if (translations.isEmpty()) return null

        val builder = StringBuilder()

        fun appendDot(withSpace: Boolean = false) {
            val space = " "
            val dot = "•"
            builder.append(space.plus(dot))
            if (withSpace) builder.append(space)
        }

        val hasBd = translations.find { it.quality == TranslationQuality.BD || it.quality == TranslationQuality.DVD } != null
        val isSameAuthor = author.isNotEmpty() && author == setting?.lastAuthor
        val canBeDownloaded = translations.find { parsers.isSupported(it.hosting) } != null


        if (isSameAuthor) {
            builder.append("  ")
            if (!hasBd) {
                val lastAuthor = context.getString(R.string.translation_last_author)
                builder.append(lastAuthor)
            }
        }
        if (hasBd) {
            if (builder.isNotEmpty()) {
                builder.replace(0, 2, "")
                appendDot(true)
            }
            val blueRay = context.getString(R.string.translation_blue_ray)
            builder.append(blueRay)
        }

        if (!hasBd && !isSameAuthor) {
            if (builder.isNotEmpty()) appendDot(true)
            val text = context.getString(R.string.translation_sources)
            builder.append(text)
        }

        val description: CharSequence? = if (builder.isNotEmpty()) builder else null
        //one chip per video: Shikicinema lists one video several times in a card (P7-T5 Q29)
        val videos = translations.map { t -> convertVideo(t) }.distinctBy { sameVideoKey(it) }

        return TranslationViewModel(
                translations.first().videoId,
                authorLabel(author),
                description,
                videos,
                isSameAuthor,
                canBeDownloaded,
                translations.first().episodesSize
        )
    }

    override fun authorLabel(author: String): String = author.trim().ifEmpty { unknownAuthor }

    override fun convertTrack(video: Video, track: Track): SeriesDownloadItem = SeriesDownloadItem(
            track,
            video,
            video.author,
            //in lower case, as it sits inside the line: «… - 07 (720p, озвучка, kodik.cc)»
            video.translationType?.labelRes?.let { context.getString(it).toLowerCase() }.orEmpty(),
            track.displayQuality.orEmpty(),
            //domain-shaped for every hosting ("vk.com", "cda.pl")
            video.hosting.synonymType
    )

    /**
     * Rows with the same key play the same video, and the first of them stays. AnimeJoy lists one file
     * under each of its domains, so its rows compare by the `file=` inside the link - a domain that
     * moves keeps its file and merges, a new file does not. Any other row compares by the very same
     * link.
     */
    private fun sameVideoKey(video: TranslationVideo): String {
        val url = video.webPlayerUrl ?: return "row " + video.videoId
        val file = if (video.videoHosting is VideoHosting.ANIMEJOY) url.toUri().getQueryParameter("file") else null
        return if (file != null) "file $file" else url
    }

    private fun convertVideo(t: Translation): TranslationVideo = TranslationVideo(
            t.videoId,
            t.animeId,
            t.episodeId,
            t.author,
            t.type,
            t.hosting,
            t.webPlayerUrl
    )

}
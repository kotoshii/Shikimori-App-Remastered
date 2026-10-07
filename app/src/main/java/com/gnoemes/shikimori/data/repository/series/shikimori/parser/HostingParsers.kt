package com.gnoemes.shikimori.data.repository.series.shikimori.parser

import android.util.Log
import com.gnoemes.shikimori.entity.app.domain.exceptions.HostingChallengeException
import com.gnoemes.shikimori.entity.series.domain.Video
import com.gnoemes.shikimori.entity.series.domain.VideoHosting
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import io.reactivex.Single
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "HostingParser"
private const val HTTP_NOT_FOUND = 404

/** Every registered [HostingParser], looked up by the hosting it handles. */
class HostingParsers @Inject constructor(
        parsers: Set<@JvmSuppressWildcards HostingParser>
) {

    private val byHosting: Map<Class<out VideoHosting>, HostingParser> = parsers.associateBy { it.hosting }

    /**
     * Whether the app can resolve real tracks for a hosting, and so offer the embedded player,
     * a quality menu and downloading.
     *
     * Anime365 counts: its parser only produces tracks for a user with an anime365 token *and* a
     * paid subscription, and everyone else gets an empty list and falls back to the web player,
     * which appends the same token.
     */
    fun isSupported(hosting: VideoHosting): Boolean = byHosting.containsKey(hosting.javaClass)

    fun getVideo(payload: TranslationVideo): Single<Video> {
        val parser = byHosting[payload.videoHosting.javaClass]
        val playerUrl = payload.webPlayerUrl

        //a hosting with no parser cannot be resolved to tracks. SeriesPresenter sends those to the
        //web player before ever calling this, so it is only a safety net - and it is what the old
        //backend did for them anyway, handing the player url straight back.
        val parsed =
                if (parser == null || playerUrl == null) Single.just(ParsedVideo.EMPTY)
                else Single.defer { parser.parse(playerUrl) }
                        .onErrorResumeNext { nothingFoundUnlessNetwork(payload, it) }

        return parsed.map {
            Video(payload.animeId, payload.episodeIndex.toLong(), playerUrl.orEmpty(),
                    payload.videoHosting, it.tracks, it.subtitles)
        }
    }

    fun headers(video: Video?): Map<String, String> =
            video?.let { byHosting[it.hosting.javaClass]?.headers(it) }.orEmpty()

    /**
     * A page that is not what a parser expects used to fail as a NullPointerException, which the
     * presenters only logged - the spinner stopped and nothing else happened. It is answered with
     * no tracks now, which the screens report as the video not being found. So is a 404: the
     * hosting deleted the video, and "HTTP error 404" over an error screen said less than that.
     *
     * Network trouble, any other http error and an anti-bot page keep their own messages.
     */
    private fun nothingFoundUnlessNetwork(payload: TranslationVideo, error: Throwable): Single<ParsedVideo> {
        val deleted = error is HttpException && error.code() == HTTP_NOT_FOUND
        if (!deleted && (error is IOException || error is HttpException || error is HostingChallengeException)) {
            return Single.error(error)
        }

        Log.w(TAG, "${payload.videoHosting.synonymType}: nothing playable in ${payload.webPlayerUrl}", error)
        return Single.just(ParsedVideo.EMPTY)
    }
}

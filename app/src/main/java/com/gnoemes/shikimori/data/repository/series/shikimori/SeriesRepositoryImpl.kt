package com.gnoemes.shikimori.data.repository.series.shikimori

import com.gnoemes.shikimori.data.local.db.AnimeRateSyncDbSource
import com.gnoemes.shikimori.data.local.db.EpisodeDbSource
import com.gnoemes.shikimori.data.network.AnimeSource
import com.gnoemes.shikimori.data.network.TopicApi
import com.gnoemes.shikimori.data.network.VideoApi
import com.gnoemes.shikimori.data.repository.series.shikimori.converter.*
import com.gnoemes.shikimori.data.repository.series.shikimori.parser.*
import com.gnoemes.shikimori.data.repository.series.smotretanime.Anime365TokenSource
import com.gnoemes.shikimori.entity.app.domain.Constants
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import okhttp3.ResponseBody
import javax.inject.Inject

class SeriesRepositoryImpl @Inject constructor(
        private val api: VideoApi,
        private val topicApi: TopicApi,
        private val source: AnimeSource,
        private val tokenSource: Anime365TokenSource,
        private val converter: EpisodeResponseConverter,
        private val translationConverter: TranslationResponseConverter,
        private val videoConverter: VideoResponseConverter,
        private val episodeSource: EpisodeDbSource,
        private val syncSource: AnimeRateSyncDbSource,
        private val vkParser: VkParser,
        private val sovetRomanticaParser: SovetRomanticaParser,
        private val sibnetParser: SibnetParser,
        private val okParser: OkParser,
        private val mailRuParser: MailRuParser,
        private val nuumParser: NuumParser,
        private val myviParser: MyviParser,
        private val allVideoParser: AllVideoParser,
        private val animeJoyParser: AnimeJoyParser,
        private val dzenParser: DzenParser,
        private val cdaParser: CdaParser
) : SeriesRepository {

    override fun getEpisodes(id: Long, name: String, alternative: Boolean): Single<List<Episode>> =
            (if (alternative) source.getEpisodesShikicinema(id) else source.getEpisodes(id, name))
                    .map { episodes -> episodes.filter { it.index > 0 }.sortedBy { it.index } }
                    .map { episodes ->
                        if (alternative || tokenSource.getToken() != null) episodes
                        else episodes.filterNot { episode ->
                            episode.hostings.any { it is VideoHosting.SMOTRET_ANIME }
                        }
                    }
                    .flatMap {
                        Observable.fromIterable(it)
                                .flatMapSingle { episode ->
                                    episodeSource.isEpisodeWatched(episode.animeId, episode.index)
                                            .map { isWatched -> converter.convertResponse(episode, isWatched) }
                                }
                                .toList()
                    }
                    .flatMap { episodes ->
                        episodeSource.saveEpisodes(episodes).toSingleDefault(episodes)
                                .flatMap { syncEpisodes(id, it) }
                    }

    override fun getTranslations(type: TranslationType, animeId: Long, episodeId: Long, name : String, alternative: Boolean, loadLength: Boolean): Single<List<Translation>> =
            (if (alternative) source.getTranslationsShikicinema(animeId, episodeId, type, loadLength) else source.getTranslations(animeId, name, episodeId, type))
                    .map(translationConverter)
                    .map { translations ->
                        if (alternative || tokenSource.getToken() != null) translations
                        else translations.filterNot { translation ->
                            translation.hosting is VideoHosting.SMOTRET_ANIME
                        }
                    }

    override fun getVideo(payload: TranslationVideo, alternative: Boolean): Single<Video> =
            when (payload.videoHosting) {
                is VideoHosting.VK -> getVkFiles(payload)
                is VideoHosting.SOVET_ROMANTICA -> getSovetRomanticaFiles(payload)
                is VideoHosting.SIBNET -> getSibnetFiles(payload)
                is VideoHosting.OK -> getOkFiles(payload)
                is VideoHosting.MAILRU -> getMailRuFiles(payload)
                is VideoHosting.NUUM -> getNuumFiles(payload)
                is VideoHosting.MYVI -> getMyviFiles(payload)
                is VideoHosting.ALLVIDEO -> getAllVideoFiles(payload)
                is VideoHosting.ANIMEJOY -> getAnimeJoyFiles(payload)
                is VideoHosting.DZEN -> getDzenVideoFiles(payload)
                is VideoHosting.CDA -> getCdaFiles(payload)
                else -> (if (alternative) source.getVideoAlternative(payload.videoId, payload.animeId, payload.episodeIndex.toLong(), tokenSource.getToken())
                    else source.getVideo(
                            payload.animeId,
                            payload.episodeIndex,
                            if (payload.videoId == Constants.NO_ID) "" else payload.videoId.toString(),
                            payload.language,
                            payload.type,
                            payload.authorSimple,
                            payload.videoHosting.synonymType,
                            payload.webPlayerUrl
                    ))
                        .map(videoConverter)
            }

    private fun getVkFiles(video: TranslationVideo): Single<Video> =
            if (video.webPlayerUrl == null) Single.just(vkParser.video(video, emptyList()))
            else api.getPlayerHtml(video.webPlayerUrl)
                    .map { vkParser.tracks(it.string()) }
                    .map { vkParser.video(video, it) }

    private fun getSovetRomanticaFiles(video: TranslationVideo): Single<Video> =
            if (video.webPlayerUrl == null) Single.just(sovetRomanticaParser.video(video, emptyList()))
            else api.getPlayerHtml(video.webPlayerUrl)
                    .map { sovetRomanticaParser.getMasterPlaylistUrl(it.string()) }
                    .flatMap {
                        Single.zip(api.getTextResponse(it), Single.just(it), BiFunction { response: ResponseBody, url: String -> Pair(response, url) })
                    }
                    .map { sovetRomanticaParser.tracks(it.first.string(), it.second) }
                    .map { sovetRomanticaParser.video(video, it) }

    private fun getSibnetFiles(video: TranslationVideo): Single<Video> =
            if (video.webPlayerUrl == null) Single.just(sibnetParser.video(video, emptyList()))
            else api.getPlayerHtml(video.webPlayerUrl)
                    .map { sibnetParser.tracks(it.string()) }
                    .map { sibnetParser.video(video, it) }

    private fun getOkFiles(video: TranslationVideo): Single<Video> =
            if (video.webPlayerUrl == null) Single.just(okParser.video(video, emptyList()))
            else api.getPlayerHtml(video.webPlayerUrl)
                    .map { okParser.tracks(it.string()) }
                    .map { okParser.video(video, it) }

    private fun getMailRuFiles(video: TranslationVideo): Single<Video> =
        if (video.webPlayerUrl == null) Single.just(mailRuParser.video(video, emptyList()))
        else api.getPlayerHtml(video.webPlayerUrl)
                .map { mailRuParser.parseVideoMetaUrl(it.string()) }
                .flatMap { api.getMailRuVideoMeta(it) }
                .map { mailRuParser.saveCookies(it) }
                .map { mailRuParser.tracks(it.body()) }
                .map { mailRuParser.video(video, it) }

    private fun getNuumFiles(video: TranslationVideo): Single<Video> =
        if (video.webPlayerUrl == null) Single.just(nuumParser.video(video, emptyList()))
        else api.getNuumStreamsMetadata(nuumParser.getMetadataUrl(video.webPlayerUrl))
                .map { nuumParser.getMasterPlaylistUrl(it.body()) }
                .flatMap { api.getTextResponse(it, "https://nuum.ru/") }
                .map { nuumParser.tracks(it.string()) }
                .map { nuumParser.video(video, it) }

    private fun getMyviFiles(video: TranslationVideo): Single<Video> =
            if (video.webPlayerUrl == null) Single.just(myviParser.video(video, emptyList()))
            else api.getPlayerHtml(video.webPlayerUrl)
                    .map { myviParser.tracks(it.string()) }
                    .map { myviParser.video(video, it) }

    private fun getAllVideoFiles(video: TranslationVideo): Single<Video> =
            if (video.webPlayerUrl == null) Single.just(allVideoParser.video(video, emptyList()))
            else api.getPlayerHtml(video.webPlayerUrl)
                    .map { allVideoParser.tracks(it.string()) }
                    .map { allVideoParser.video(video, it) }

    private fun getAnimeJoyFiles(video: TranslationVideo): Single<Video> =
            Single.just(animeJoyParser.video(video, animeJoyParser.tracks(video.webPlayerUrl)))

    private fun getDzenVideoFiles(video: TranslationVideo): Single<Video> =
            if (video.webPlayerUrl == null) Single.just(dzenParser.video(video, emptyList()))
            else api.getPlayerHtml(video.webPlayerUrl)
                    .map { dzenParser.getMasterPlaylistUrl(it.string()) }
                    .flatMap {
                        Single.zip(api.getTextResponse(it), Single.just(it), BiFunction { response: ResponseBody, url: String -> Pair(response, url) })
                    }
                    .map { dzenParser.tracks(it.first.string(), it.second) }
                    .map { dzenParser.video(video, it) }

    private fun getCdaFiles(video: TranslationVideo): Single<Video> =
            if (video.webPlayerUrl == null) Single.just(cdaParser.video(video, emptyList()))
            else api.getPlayerHtml(video.webPlayerUrl)
                    .map { cdaParser.parsePlayerData(it.string()) }
                    .flatMap { cdaParser.getVideoLinks(it) }
                    .map { cdaParser.tracks(it) }
                    .map { cdaParser.video(video, it) }

    override fun getTopic(animeId: Long, episodeId: Int): Single<Long> =
            topicApi.getAnimeEpisodeTopic(animeId, episodeId)
                    .map { list -> list.firstOrNull { it.episode?.toIntOrNull() == episodeId }?.id }

    override fun getFirstNotWatchedEpisodeIndex(animeId: Long): Single<Int> = episodeSource.getFirstNotWatchedEpisodeIndex(animeId)

    override fun getWatchedEpisodesCount(animeId: Long): Single<Int> = episodeSource.getWatchedEpisodesCount(animeId)

    private fun syncEpisodes(id: Long, list: List<Episode>): Single<List<Episode>> {
        return Single.fromCallable { list }
                .flatMap { episodes ->
                    Single.zip(episodeSource.getWatchedEpisodesCount(id), syncSource.getEpisodeCount(id), BiFunction<Int, Int, Boolean> { local, remote -> local == remote })
                            .flatMap { same -> if (same) Single.fromCallable { episodes } else updateFromSync(id, episodes) }
                }
    }

    private fun updateFromSync(id: Long, episodes: List<Episode>): Single<List<Episode>> {
        return Single.zip(episodeSource.getWatchedEpisodesCount(id), syncSource.getEpisodeCount(id), BiFunction<Int, Int, Int> { local, remote -> remote.minus(local) })
                .flatMap {
                    if (it > 0) increaseLocal(it, episodes)
                    else decreaseLocal(Math.abs(it), episodes)
                }
                .map { newStatusEpisodes ->
                    episodes.map { episode ->
                        newStatusEpisodes
                                .find { episode.index == it.index }
                                ?.let { episode.copy(isWatched = it.isWatched) }
                                ?: episode
                    }
                }
    }

    private fun decreaseLocal(count: Int, episodes: List<Episode>): Single<List<Episode>> =
            Observable.fromIterable(
                    episodes.asSequence()
                            .sortedByDescending { it.index }
                            .filter { it.isWatched }
                            .toMutableList()
                            .take(count))
                    .flatMapSingle { episodeSource.episodeUnWatched(it.animeId, it.index).toSingleDefault(it) }
                    .flatMapSingle { updateEpisode(it) }
                    .toList()


    private fun increaseLocal(count: Int, episodes: List<Episode>): Single<List<Episode>> =
            Observable.fromIterable(
                    episodes.asSequence()
                            .sortedBy { it.index }
                            .filter { !it.isWatched }
                            .toMutableList()
                            .take(count))
                    .flatMapSingle { episodeSource.episodeWatched(it.animeId, it.index).toSingleDefault(it) }
                    .flatMapSingle { updateEpisode(it) }
                    .toList()

    private fun updateEpisode(episode: Episode): Single<Episode> {
        return episodeSource.isEpisodeWatched(episode.animeId, episode.index)
                .map { episode.copy(isWatched = it) }
    }
}


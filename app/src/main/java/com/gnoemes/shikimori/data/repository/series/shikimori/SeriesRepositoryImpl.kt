package com.gnoemes.shikimori.data.repository.series.shikimori

import com.gnoemes.shikimori.data.local.db.AnimeRateSyncDbSource
import com.gnoemes.shikimori.data.local.db.EpisodeDbSource
import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.network.AnimeSource
import com.gnoemes.shikimori.data.network.TopicApi
import com.gnoemes.shikimori.data.repository.series.shikimori.converter.*
import com.gnoemes.shikimori.data.repository.series.shikimori.parser.HostingParsers
import com.gnoemes.shikimori.data.repository.series.smotretanime.Anime365TokenSource
import com.gnoemes.shikimori.entity.series.domain.*
import com.gnoemes.shikimori.entity.series.presentation.TranslationVideo
import com.gnoemes.shikimori.utils.HostingFilter
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import javax.inject.Inject

private const val UNKNOWN_HOSTING = "unknown"

class SeriesRepositoryImpl @Inject constructor(
        private val topicApi: TopicApi,
        private val source: AnimeSource,
        private val tokenSource: Anime365TokenSource,
        private val settingsSource: SettingsSource,
        private val converter: EpisodeResponseConverter,
        private val translationConverter: TranslationResponseConverter,
        private val episodeSource: EpisodeDbSource,
        private val syncSource: AnimeRateSyncDbSource,
        private val parsers: HostingParsers
) : SeriesRepository {

    override fun getEpisodes(id: Long, name: String, alternative: Boolean): Single<List<Episode>> =
            (if (alternative) source.getEpisodesShikicinema(id) else source.getEpisodes(id, name))
                    .map { episodes -> episodes.filter { it.index > 0 }.sortedBy { it.index } }
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
                        if (settingsSource.hideAnime365 && tokenSource.getToken() == null)
                            translations.filterNot { translation -> translation.hosting is VideoHosting.SMOTRET_ANIME }
                        else translations
                    }
                    .map(::rememberHostings)
                    .map(::filterHiddenHostings)

    override fun getVideo(payload: TranslationVideo): Single<Video> =
            parsers.getVideo(payload)
                    //the parsers only know about tracks, so the translation's own details are
                    //attached here rather than in each of them
                    .map { it.copy(author = payload.author, translationType = payload.type) }

    /**
     * Remembers every hosting the user is shown, so the filter screen has real names to offer -
     * a fixed list of the `VideoHosting` subclasses would miss most of them, since anything the app
     * does not recognise arrives as `UNKNOWN` carrying the raw name.
     *
     * Runs on every translations load, so it only writes when something genuinely new turns up.
     */
    private fun rememberHostings(translations: List<Translation>): List<Translation> {
        val seen = settingsSource.seenHostings
        val found = translations
                .map { it.hosting.synonymType }
                //"unknown" is the placeholder for a translation with no hosting at all, not a name
                .filter { it.isNotBlank() && it != UNKNOWN_HOSTING }

        if (!seen.containsAll(found)) settingsSource.seenHostings = seen + found

        return translations
    }

    private fun filterHiddenHostings(translations: List<Translation>): List<Translation> {
        val hidden = settingsSource.hiddenHostings
        if (hidden.isEmpty()) return translations

        return translations.filterNot { HostingFilter.isHidden(it.hosting.synonymType, hidden) }
    }

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


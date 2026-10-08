package com.gnoemes.shikimori.domain.series

import com.gnoemes.shikimori.data.local.preference.SettingsSource
import com.gnoemes.shikimori.data.local.progress.WatchProgressStore
import com.gnoemes.shikimori.data.repository.rates.RatesRepository
import com.gnoemes.shikimori.data.repository.user.UserRepository
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.entity.series.domain.WatchProgress
import com.gnoemes.shikimori.entity.user.domain.UserStatus
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.HttpException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

private const val HTTP_NOT_FOUND = 404

/**
 * Every write is decided twice: first against the count in hand, so nothing is sent when there is
 * nothing to change, then against a fresh read, so a count changed on the site or another phone in
 * the meantime is neither lowered nor counted past. Shikimori has no "only if higher" write.
 */
class WatchProgressInteractorImpl @Inject constructor(
        private val store: WatchProgressStore,
        private val ratesRepository: RatesRepository,
        private val userRepository: UserRepository,
        private val settingsSource: SettingsSource
) : WatchProgressInteractor {

    //the built-in player reports an episode again on a quality switch, and the two reports could
    //otherwise both read the old count and add 1 each
    private val writing = ConcurrentHashMap<Long, Boolean>()

    override fun remember(animeId: Long, rateId: Long?, watchedEpisodes: Int) {
        store.put(animeId, WatchProgress(rateId, watchedEpisodes))
    }

    override fun watchedEpisodes(animeId: Long): Int = store.watchedEpisodes(animeId)

    override fun episodePlayed(animeId: Long, episode: Int): Completable {
        if (!settingsSource.isAutoIncrement || !isSignedIn()) return Completable.complete()
        if (episode != watchedEpisodes(animeId) + 1) return Completable.complete()

        return write(animeId) { current ->
            when {
                //progress appearing is what «Автоматическое добавление» adds a title on, and with
                //nothing in the list the next episode is the first
                current.rateId == null ->
                    if (settingsSource.isAutoStatus && episode == 1) create(animeId, episode)
                    else Completable.complete()
                episode == current.episodes + 1 ->
                    ratesRepository.increment(current.rateId)
                            .doOnComplete { store.put(animeId, current.copy(episodes = episode)) }
                else -> Completable.complete()
            }
        }
    }

    override fun markWatchedUpTo(animeId: Long, episode: Int): Completable {
        if (!isSignedIn() || episode <= watchedEpisodes(animeId)) return Completable.complete()

        return write(animeId) { current ->
            when {
                //asked for in so many words, so the title is added whatever «Автоматическое
                //добавление» says - as it was before
                current.rateId == null -> create(animeId, episode)
                episode > current.episodes ->
                    ratesRepository.updateRate(UserRate(id = current.rateId, targetId = animeId, targetType = Type.ANIME, episodes = episode))
                            .doOnComplete { store.put(animeId, current.copy(episodes = episode)) }
                else -> Completable.complete()
            }
        }
    }

    private fun write(animeId: Long, change: (WatchProgress) -> Completable): Completable =
            Completable.defer {
                if (writing.putIfAbsent(animeId, true) != null) Completable.complete()
                else read(animeId)
                        .flatMapCompletable { change(it) }
                        .doFinally { writing.remove(animeId) }
            }.applyErrorHandlerAndSchedulers()

    /** The entry as shikimori has it now: by id when one is known, otherwise looked up by anime. */
    private fun read(animeId: Long): Single<WatchProgress> {
        val rateId = store.get(animeId)?.rateId

        val fresh =
                if (rateId == null) lookUp(animeId)
                else ratesRepository.getRate(rateId)
                        .map { WatchProgress(it.id, it.episodes ?: 0) }
                        //deleted on the site since - it may have been added back under a new id
                        .onErrorResumeNext { if (it is HttpException && it.code() == HTTP_NOT_FOUND) lookUp(animeId) else Single.error(it) }

        return fresh.doOnSuccess { store.put(animeId, it) }
    }

    private fun lookUp(animeId: Long): Single<WatchProgress> =
            userRepository.getMyUserId()
                    .flatMap { ratesRepository.getUserRates(it, animeId, Type.ANIME) }
                    .map { rates -> rates.firstOrNull()?.let { WatchProgress(it.id, it.episodes ?: 0) } ?: WatchProgress.NOT_IN_LIST }

    private fun create(animeId: Long, episodes: Int): Completable =
            userRepository.getMyUserId()
                    .flatMap { ratesRepository.createRateWithResult(animeId, Type.ANIME, UserRate(status = RateStatus.WATCHING, episodes = episodes), it) }
                    .doOnSuccess { store.put(animeId, WatchProgress(it.id, it.episodes ?: episodes)) }
                    .ignoreElement()

    private fun isSignedIn(): Boolean = userRepository.getUserStatus() == UserStatus.AUTHORIZED
}

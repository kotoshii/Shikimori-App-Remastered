package com.gnoemes.shikimori.data.repository.rates

import com.gnoemes.shikimori.data.network.UserApi
import com.gnoemes.shikimori.data.repository.common.RateResponseConverter
import com.gnoemes.shikimori.data.repository.user.UserRepository
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.data.RateResponse
import com.gnoemes.shikimori.entity.rates.domain.Rate
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.entity.user.domain.UserStatus
import com.gnoemes.shikimori.utils.dropExtraPageItem
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class RatesRepositoryImpl @Inject constructor(
        private val api: UserApi,
        private val converter: RateResponseConverter,
        private val userRepository: UserRepository
) : RatesRepository {

    override fun getAnimeRates(id: Long, page: Int, limit: Int, rateStatus: RateStatus): Single<List<Rate>> =
            rates(api.getUserAnimeRates(id, page, limit, rateStatus.status), limit)

    override fun getMangaRates(id: Long, page: Int, limit: Int, rateStatus: RateStatus): Single<List<Rate>> =
            rates(api.getUserMangaRates(id, page, limit, rateStatus.status), limit)

    private fun rates(request: Single<List<RateResponse>>, limit: Int): Single<List<Rate>> =
            request
                    .map { it.dropExtraPageItem(limit) }
                    .map(converter)
                    .onErrorResumeNext { if (it is NoSuchElementException) Single.just(emptyList()) else Single.error(it) }

    override fun getUserRates(id: Long, targetId: Long?, target: Type?, statuses: String?, page: Int, limit: Int): Single<List<UserRate>> =
            api.getUserRates(id, targetId, target?.let { targetType(it) }, statuses, page, limit)
                    .map { list -> list.mapNotNull { converter.convertUserRateResponse(targetId, it) } }

    override fun getMyRates(type: Type, statuses: List<RateStatus>?): Single<Map<Long, UserRate>> =
            Single.defer {
                if (userRepository.getUserStatus() != UserStatus.AUTHORIZED) Single.just(emptyMap())
                else userRepository.getMyUserId()
                        .flatMap { getUserRates(it, target = type, statuses = statuses?.joinToString(",") { status -> status.status }) }
                        .map { rates -> rates.filter { it.targetId != null }.associateBy { it.targetId!! } }
            }

    //the api knows only Anime and Manga: a ranobe is a manga to it, and "Ranobe" gets nothing back
    private fun targetType(type: Type): String = if (type == Type.ANIME) "Anime" else "Manga"

    override fun createRate(id: Long, type: Type, rate: UserRate, userId: Long): Completable =
            createRateWithResult(id, type, rate, userId).ignoreElement()

    //sends what the user typed. Both used to send a count from a local table instead: for anime the
    //ticked episodes, for manga a table nothing ever wrote, so every new entry started at 0
    override fun createRateWithResult(id: Long, type: Type, rate: UserRate, userId: Long): Single<UserRate> =
            when (type) {
                Type.ANIME -> create(id, Type.ANIME, rate, userId)
                //a ranobe is a manga to the api
                Type.MANGA, Type.RANOBE -> create(id, Type.MANGA, rate, userId)
                else -> Single.error(IllegalStateException())
            }

    private fun create(id: Long, type: Type, rate: UserRate, userId: Long): Single<UserRate> =
            api.createRate(converter.convertCreateOrUpdateRequest(id, type, rate, userId))
                    .map { converter.convertUserRateResponse(id, it) }

    override fun updateRate(rate: UserRate): Completable =
            api.updateRate(rate.id!!, converter.convertCreateOrUpdateRequest(rate))
                    .ignoreElement()

    override fun deleteRate(id: Long): Completable = api.deleteRate(id)

    override fun increment(rateId: Long): Completable = api.increment(rateId)

    override fun getRate(id: Long): Single<UserRate> =
            api.getRate(id)
                    .map { converter.convertUserRateResponse(null, it) }

}

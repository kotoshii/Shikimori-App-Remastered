package com.gnoemes.shikimori.data.repository.rates

import com.gnoemes.shikimori.data.network.UserApi
import com.gnoemes.shikimori.data.repository.common.RateResponseConverter
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.Rate
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.utils.firstUpperCase
import io.reactivex.Completable
import io.reactivex.Single
import javax.inject.Inject

class RatesRepositoryImpl @Inject constructor(
        private val api: UserApi,
        private val converter: RateResponseConverter
) : RatesRepository {

    override fun getAnimeRates(id: Long, page: Int, limit: Int, rateStatus: RateStatus): Single<List<Rate>> =
            api.getUserAnimeRates(id, page, limit, rateStatus.status)
                    .map(converter)
                    .onErrorResumeNext { if (it is NoSuchElementException) Single.just(emptyList()) else Single.error(it) }
                    .doOnSuccess { if (it.isNotEmpty() && page > 1) it.toMutableList().removeAt(0) }

    override fun getMangaRates(id: Long, page: Int, limit: Int, rateStatus: RateStatus): Single<List<Rate>> =
            api.getUserMangaRates(id, page, limit, rateStatus.status)
                    .map(converter)
                    .onErrorResumeNext { if (it is NoSuchElementException) Single.just(emptyList()) else Single.error(it) }
                    .doOnSuccess { if (it.isNotEmpty() && page > 1) it.toMutableList().removeAt(0) }

    override fun getUserRates(id: Long, targetId: Long?, target: Type?, statuses: String?, page: Int, limit: Int): Single<List<UserRate>> =
            api.getUserRates(id, targetId, target?.name?.toLowerCase()?.firstUpperCase(), statuses, page, limit)
                    .map { list -> list.mapNotNull { converter.convertUserRateResponse(targetId, it) } }

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

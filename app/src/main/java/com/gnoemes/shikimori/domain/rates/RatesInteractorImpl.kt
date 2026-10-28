package com.gnoemes.shikimori.domain.rates

import com.gnoemes.shikimori.data.repository.rates.RatesRepository
import com.gnoemes.shikimori.data.repository.user.UserRepository
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.Rate
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.utils.applyErrorHandlerAndSchedulers
import com.gnoemes.shikimori.utils.hiddenListOnForbidden
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import javax.inject.Inject

class RatesInteractorImpl @Inject constructor(
        private val repository: RatesRepository,
        private val userRepository: UserRepository
) : RatesInteractor {

    override fun getAnimeRates(id: Long, page: Int, limit: Int, rateStatus: RateStatus): Single<Page<Rate>> =
            repository.getAnimeRates(id, page, limit, rateStatus)
                    .applyErrorHandlerAndSchedulers()
                    .hiddenListOnForbidden(this::isGuest)

    override fun getMangaRates(id: Long, page: Int, limit: Int, rateStatus: RateStatus): Single<Page<Rate>> =
            repository.getMangaRates(id, page, limit, rateStatus)
                    .applyErrorHandlerAndSchedulers()
                    .hiddenListOnForbidden(this::isGuest)

    private fun isGuest(): Boolean = !userRepository.isSignedIn()

    override fun getRate(id: Long): Single<UserRate> = repository.getRate(id).applyErrorHandlerAndSchedulers()

    override fun getMyRate(targetId: Long, type: Type): Maybe<UserRate> =
            repository.getMyRate(targetId, type).applyErrorHandlerAndSchedulers()

    override fun deleteRate(id: Long): Completable = repository.deleteRate(id).applyErrorHandlerAndSchedulers()

    override fun createRate(id: Long, type: Type, rate: UserRate): Completable =
            userRepository.getMyUserId()
                    .flatMapCompletable { repository.createRate(id, type, rate, it) }
                    .applyErrorHandlerAndSchedulers()

    override fun createRateWithResult(id: Long, type: Type, status: RateStatus): Single<UserRate> =
            userRepository.getMyUserId()
                    .flatMap { repository.createRateWithResult(id, type, UserRate(status = status), it) }
                    .applyErrorHandlerAndSchedulers()

    override fun updateRate(rate: UserRate): Completable =
            repository.updateRate(rate)
                    .applyErrorHandlerAndSchedulers()

    override fun changeRateStatus(rateId: Long, newStatus: RateStatus): Completable =
            repository.getRate(rateId)
                    .flatMapCompletable { repository.updateRate(it.copy(status = newStatus)) }
                    .applyErrorHandlerAndSchedulers()

    override fun increment(rateId: Long): Completable =
            repository.increment(rateId)
                    .applyErrorHandlerAndSchedulers()

}
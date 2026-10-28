package com.gnoemes.shikimori.domain.rates

import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.domain.Rate
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import com.gnoemes.shikimori.entity.rates.domain.UserRate
import com.gnoemes.shikimori.entity.common.domain.Page
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

interface RatesInteractor {

    fun getAnimeRates(id: Long, page: Int, limit: Int, rateStatus: RateStatus): Single<Page<Rate>>

    fun getMangaRates(id: Long, page: Int, limit: Int, rateStatus: RateStatus): Single<Page<Rate>>

    fun getRate(id : Long) : Single<UserRate>

    /** The signed-in user's entry for one title, as shikimori has it now; none when it is not in the list. */
    fun getMyRate(targetId: Long, type: Type): Maybe<UserRate>

    fun deleteRate(id: Long): Completable

    /** Adds the title to the signed-in user's list. */
    fun createRate(id: Long, type: Type, rate: UserRate): Completable

    fun createRateWithResult(id: Long, type: Type, status: RateStatus) : Single<UserRate>

    fun updateRate(rate: UserRate): Completable

    fun increment(rateId: Long): Completable

    fun changeRateStatus(rateId: Long, newStatus: RateStatus): Completable
}
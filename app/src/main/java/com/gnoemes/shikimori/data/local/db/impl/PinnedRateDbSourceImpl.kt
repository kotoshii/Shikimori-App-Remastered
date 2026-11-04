package com.gnoemes.shikimori.data.local.db.impl

import com.gnoemes.shikimori.data.local.db.PinnedRateDbSource
import com.gnoemes.shikimori.data.local.db.dao.PinnedRateDao
import com.gnoemes.shikimori.entity.common.domain.Type
import com.gnoemes.shikimori.entity.rates.data.PinnedRateEntity
import com.gnoemes.shikimori.entity.rates.domain.PinnedRate
import com.gnoemes.shikimori.entity.rates.domain.RateStatus
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/** Room runs a call on the thread that subscribes and refuses the main one, so each call moves to io. */
class PinnedRateDbSourceImpl @Inject constructor(
        private val dao: PinnedRateDao
) : PinnedRateDbSource {

    override fun getPinnedRates(type: Type, status: RateStatus): Single<List<PinnedRate>> =
            dao.getPinnedRates(type.name, status.status)
                    //a NULL order reads as 0, as it always has
                    .map { rows -> rows.map { PinnedRate(it.id, type, status, it.order ?: 0) } }
                    .subscribeOn(Schedulers.io())

    override fun addPinnedRate(rate: PinnedRate): Completable =
            dao.addPinnedRate(PinnedRateEntity(rate.id, rate.type.name, rate.status.status, rate.order))
                    .subscribeOn(Schedulers.io())

    override fun removePinnedRate(id: Long): Completable =
            dao.removePinnedRate(id)
                    .subscribeOn(Schedulers.io())
}

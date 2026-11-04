package com.gnoemes.shikimori.data.local.db.impl

import com.gnoemes.shikimori.data.local.db.TranslationSettingDbSource
import com.gnoemes.shikimori.data.local.db.dao.TranslationSettingDao
import com.gnoemes.shikimori.entity.series.data.TranslationSettingEntity
import com.gnoemes.shikimori.entity.series.domain.TranslationSetting
import com.gnoemes.shikimori.entity.series.domain.TranslationType
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/** Room runs a call on the thread that subscribes and refuses the main one, so each call moves to io. */
class TranslationSettingDbSourceImpl @Inject constructor(
        private val dao: TranslationSettingDao
) : TranslationSettingDbSource {

    //written when subscribed - not on the thread that builds the chain
    override fun saveSetting(setting: TranslationSetting): Completable =
            dao.saveSetting(TranslationSettingEntity(setting.animeId, setting.lastAuthor, setting.lastType?.type))
                    .subscribeOn(Schedulers.io())

    override fun getSetting(animeId: Long): Single<TranslationSetting> =
            dao.getSetting(animeId)
                    .map { row -> TranslationSetting(row.animeId, row.author, TranslationType.values().find { it.type == row.type }) }
                    .toSingle(getDefaultItem(animeId))
                    .onErrorReturnItem(getDefaultItem(animeId))
                    .subscribeOn(Schedulers.io())

    private fun getDefaultItem(animeId: Long): TranslationSetting = TranslationSetting(animeId, null, null)
}

package com.gnoemes.shikimori.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gnoemes.shikimori.data.local.db.table.PinnedRateTable
import com.gnoemes.shikimori.entity.rates.data.PinnedRateEntity
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface PinnedRateDao {

    @Query("SELECT * FROM ${PinnedRateTable.TABLE} WHERE ${PinnedRateTable.COLUMN_TYPE} = :type AND ${PinnedRateTable.COLUMN_STATUS} = :status")
    fun getPinnedRates(type: String, status: String): Single<List<PinnedRateEntity>>

    //the row with the same title, type and status, if any, is replaced
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addPinnedRate(rate: PinnedRateEntity): Completable

    //the title's rows of every type and status
    @Query("DELETE FROM ${PinnedRateTable.TABLE} WHERE ${PinnedRateTable.COLUMN_ID} = :id")
    fun removePinnedRate(id: Long): Completable
}

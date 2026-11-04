package com.gnoemes.shikimori.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gnoemes.shikimori.data.local.db.table.TranslationSettingTable
import com.gnoemes.shikimori.entity.series.data.TranslationSettingEntity
import io.reactivex.Completable
import io.reactivex.Maybe

@Dao
interface TranslationSettingDao {

    //empty when nothing is saved for the anime
    @Query("SELECT * FROM ${TranslationSettingTable.TABLE} WHERE ${TranslationSettingTable.COLUMN_ANIME_ID} = :animeId")
    fun getSetting(animeId: Long): Maybe<TranslationSettingEntity>

    //the anime's earlier row, if any, is replaced
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSetting(setting: TranslationSettingEntity): Completable
}

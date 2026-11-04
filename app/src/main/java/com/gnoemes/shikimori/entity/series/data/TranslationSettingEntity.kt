package com.gnoemes.shikimori.entity.series.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gnoemes.shikimori.data.local.db.table.TranslationSettingTable

/**
 * A row of the `translation_setting` table: the translation last chosen for an anime. Room checks a
 * database made before Room against this class when it opens it, so the names and the nullability
 * stay exactly as on disk (see AppDatabase).
 */
@Entity(tableName = TranslationSettingTable.TABLE)
data class TranslationSettingEntity(
        @PrimaryKey @ColumnInfo(name = TranslationSettingTable.COLUMN_ANIME_ID) val animeId: Long,
        @ColumnInfo(name = TranslationSettingTable.COLUMN_AUTHOR) val author: String?,
        @ColumnInfo(name = TranslationSettingTable.COLUMN_TYPE) val type: String?
)

package com.gnoemes.shikimori.entity.rates.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.gnoemes.shikimori.data.local.db.table.PinnedRateTable

/**
 * A row of the `pinned` table. Room checks a database made before Room against this class when it
 * opens it, so the names, the key's order and the nullability stay exactly as on disk (see AppDatabase).
 */
@Entity(
        tableName = PinnedRateTable.TABLE,
        primaryKeys = [PinnedRateTable.COLUMN_ID, PinnedRateTable.COLUMN_TYPE, PinnedRateTable.COLUMN_STATUS]
)
data class PinnedRateEntity(
        @ColumnInfo(name = PinnedRateTable.COLUMN_ID) val id: Long,
        @ColumnInfo(name = PinnedRateTable.COLUMN_TYPE) val type: String,
        @ColumnInfo(name = PinnedRateTable.COLUMN_STATUS) val status: String,
        //the column has always allowed NULL
        @ColumnInfo(name = PinnedRateTable.COLUMN_ORDER) val order: Int?
)

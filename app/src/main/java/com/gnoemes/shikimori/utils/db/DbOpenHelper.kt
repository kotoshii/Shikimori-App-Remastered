package com.gnoemes.shikimori.utils.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.gnoemes.shikimori.data.local.db.table.*
import javax.inject.Inject

class DbOpenHelper @Inject constructor(
        context: Context
) : SQLiteOpenHelper(context, DATABASE, null, VERSION) {

    companion object {
        const val DATABASE = "shikimori_database"
        const val VERSION = 4
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.apply {
            execSQL(AnimeRateSyncTable.CREATE_QUERY)
            execSQL(EpisodeTable.CREATE_QUERY)
            execSQL(TranslationSettingTable.CREATE_QUERY)
            execSQL(PinnedRateTable.CREATE_QUERY)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, old: Int, new: Int) {
        //version 2 created `chapters`; it is no longer created, and version 4 drops it
        if (old < 3) {
            db?.execSQL(PinnedRateTable.CREATE_QUERY)
        }

        //the manga tables: nothing ever wrote `chapters`, and `manga_rate_sync` was written but
        //never read
        if (old < 4) {
            db?.execSQL("DROP TABLE IF EXISTS chapters")
            db?.execSQL("DROP TABLE IF EXISTS manga_rate_sync")
        }
    }


}
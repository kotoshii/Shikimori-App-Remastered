package com.gnoemes.shikimori.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gnoemes.shikimori.data.local.db.dao.PinnedRateDao
import com.gnoemes.shikimori.data.local.db.dao.TranslationSettingDao
import com.gnoemes.shikimori.entity.rates.data.PinnedRateEntity
import com.gnoemes.shikimori.entity.series.data.TranslationSettingEntity

/**
 * The app's database, [NAME]. Versions 1 to 5 were made before Room, by hand-written SQL (StorIO). Room
 * has no record of such a database, so on the first open it compares every table with its entity -
 * names, NOT NULL, the primary key's columns in order - and refuses the database on any difference.
 * So an entity changes only with a new version and a migration; Room writes each version's schema into
 * app/schemas.
 */
@Database(entities = [PinnedRateEntity::class, TranslationSettingEntity::class], version = 5)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pinnedRateDao(): PinnedRateDao

    abstract fun translationSettingDao(): TranslationSettingDao

    companion object {
        const val NAME = "shikimori_database"

        /** One step per version, as the database before Room had them. */
        @JvmField
        val MIGRATIONS: Array<Migration> = arrayOf(
                //version 2 created `chapters`; it is no longer created, and version 4 drops it
                migration(1, 2) {},
                //the table as version 3 created it
                migration(2, 3) {
                    execSQL("CREATE TABLE pinned(_id INTEGER NOT NULL, type TEXT NOT NULL, status TEXT NOT NULL, pin_order INTEGER, PRIMARY KEY (_id , type, status));")
                },
                //the manga tables: nothing ever wrote `chapters`, and `manga_rate_sync` was written but
                //never read
                migration(3, 4) {
                    execSQL("DROP TABLE IF EXISTS chapters")
                    execSQL("DROP TABLE IF EXISTS manga_rate_sync")
                },
                //the anime tables: locally ticked episodes and a copy of each rate's count, replaced by
                //shikimori's own count, see WatchProgressStore
                migration(4, 5) {
                    execSQL("DROP TABLE IF EXISTS episodes")
                    execSQL("DROP TABLE IF EXISTS anime_rate_sync")
                }
        )

        private fun migration(from: Int, to: Int, steps: SupportSQLiteDatabase.() -> Unit): Migration =
                object : Migration(from, to) {
                    override fun migrate(database: SupportSQLiteDatabase) = database.steps()
                }
    }
}

package com.gnoemes.shikimori.di.app.module.local;

import android.content.Context;

import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.gnoemes.shikimori.data.local.db.AppDatabase;
import com.gnoemes.shikimori.data.local.db.dao.PinnedRateDao;
import com.gnoemes.shikimori.data.local.db.dao.TranslationSettingDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public interface DbModule {

    @Provides
    @Singleton
    static AppDatabase provideDatabase(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, AppDatabase.NAME)
                .addMigrations(AppDatabase.MIGRATIONS)
                //one file, as before Room: Room's default (WAL) keeps the latest writes in a second file,
                //shikimori_database-wal, which Android's own backup may not copy before Android 9
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build();
    }

    @Provides
    static PinnedRateDao providePinnedRateDao(AppDatabase database) {
        return database.pinnedRateDao();
    }

    @Provides
    static TranslationSettingDao provideTranslationSettingDao(AppDatabase database) {
        return database.translationSettingDao();
    }
}

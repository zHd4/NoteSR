/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import android.content.Context;
import android.util.Log;

import androidx.room.Room;

import net.sqlcipher.database.SupportFactory;

import java.util.Arrays;

import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DatabaseManagerImpl implements DatabaseManager {
    private static final String TAG = DatabaseManagerImpl.class.getCanonicalName();
    private final Context context;

    @Override
    public AppDatabase getDatabase(String name, byte[] key) {
        return Room.databaseBuilder(context, AppDatabase.class, name)
                .openHelperFactory(new SupportFactory(Arrays.copyOf(key, key.length)))
                .build();
    }

    @Override
    public void closeProvider() {
        DatabaseProvider.close();
    }

    @Override
    public void reinitProvider(byte[] key) {
        DatabaseProvider.reinit(context, new SupportFactory(Arrays.copyOf(key, key.length)));
    }

    @Override
    public boolean isDbAvailable(AppDatabase db) {
        try {
            db.getOpenHelper()
                    .getWritableDatabase()
                    .query("SELECT count(*) FROM sqlite_master"
                    ).close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Database is not available", e);
            return false;
        }
    }
}

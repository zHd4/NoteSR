/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.service.migration;

import android.content.Context;
import android.content.SharedPreferences;

public final class DataVersionManager {
    public static final int DEFAULT_FIRST_VERSION = 0;
    private static final String PREF_NAME = "migration_prefs";
    private static final String KEY_DATA_VERSION = "data_schema_version";

    private final SharedPreferences prefs;

    public DataVersionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getCurrentVersion() {
        return prefs.getInt(KEY_DATA_VERSION, DEFAULT_FIRST_VERSION);
    }

    public void setCurrentVersion(int version) {
        prefs.edit().putInt(KEY_DATA_VERSION, version).apply();
    }
}

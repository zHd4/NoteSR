package com.peew.notesr.db.notes;

import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.peew.notesr.App;
import com.peew.notesr.tools.VersionFetcher;

public class NotesDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String NAME_FORMAT = "notes_v%s";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TEXT = "text";
    private final String databaseName;

    private static String fetchDatabaseName() {
        try {
            String version = VersionFetcher.fetchVersionName(App.getContext(), true);
            return String.format(NAME_FORMAT, version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("fetchDatabaseName", e.toString());
            throw new RuntimeException(e);
        }
    }

    public NotesDatabase() {
        super(App.getContext(), fetchDatabaseName(), null, DATABASE_VERSION);
        this.databaseName = fetchDatabaseName();
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s(%s TEXT, %s TEXT)",
                databaseName, KEY_TITLE, KEY_TEXT));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}

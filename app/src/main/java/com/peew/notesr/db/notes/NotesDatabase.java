package com.peew.notesr.db.notes;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.tools.VersionFetcher;

public class NotesDatabase extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    private static final String NAME_FORMAT = "notes_v%s";
    private static final String KEY_TITLE = "title";
    private static final String KEY_TEXT = "text";

    private final Context context;

    private final String databaseName;

    public NotesDatabase(Context context) throws PackageManager.NameNotFoundException {
        super(context,
                String.format(NAME_FORMAT, VersionFetcher.fetchVersionName(context, true)),
                null,
                DATABASE_VERSION);
        this.context = context;
        this.databaseName = String.format(NAME_FORMAT, VersionFetcher
                .fetchVersionName(context, true));
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s(%s TEXT, %s TEXT)",
                databaseName, KEY_TITLE, KEY_TEXT));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        assert true;
    }
}

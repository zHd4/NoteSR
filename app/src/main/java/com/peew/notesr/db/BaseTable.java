package com.peew.notesr.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.peew.notesr.App;

import java.time.format.DateTimeFormatter;

public abstract class BaseTable {
    protected SQLiteOpenHelper helper;
    protected String name;

    public BaseTable(SQLiteOpenHelper helper, String name) {
        this.helper = helper;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Long getRowsCount() {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + getName(), null);

        try (cursor) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }

        return null;
    }

    protected DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}

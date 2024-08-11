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

    public long getRowsCount() {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + getName(), null);
        Long count = null;

        try (cursor) {
            if (cursor.moveToFirst()) {
                count = cursor.getLong(0);
            }
        }

        if (count == null) {
            throw new NullPointerException();
        }

        return count;
    }

    public void deleteAll() {
        helper.getWritableDatabase()
                .delete(name, null, null);
    }

    protected DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}

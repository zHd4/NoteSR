package com.peew.notesr.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.peew.notesr.App;

import java.time.format.DateTimeFormatter;

public abstract class BaseTable {

    protected final String name;
    protected final BaseDB.Databases databases;

    public BaseTable(BaseDB.Databases databases, String name) {
        this.name = name;
        this.databases = databases;
    }

    public String getName() {
        return name;
    }

    public long getRowsCount() {
        Cursor cursor = databases.readable.rawQuery("SELECT COUNT(*) FROM " + getName(), null);
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
        databases.writable.delete(name, null, null);
    }

    protected DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}

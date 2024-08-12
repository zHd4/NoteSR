package com.peew.notesr.db;

import android.database.Cursor;
import com.peew.notesr.App;

import java.time.format.DateTimeFormatter;

public abstract class BaseTable {

    protected final BaseDB db;
    protected final String name;

    public BaseTable(BaseDB db, String name) {
        this.db = db;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public long getRowsCount() {
        Cursor cursor = db.readableDatabase.rawQuery("SELECT COUNT(*) FROM " + getName(), null);
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
        db.writableDatabase.delete(name, null, null);
    }

    protected DateTimeFormatter getTimestampFormatter() {
        return App.getAppContainer().getTimestampFormatter();
    }
}

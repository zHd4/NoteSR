package com.peew.notesr.db;

import android.database.sqlite.SQLiteOpenHelper;
import com.peew.notesr.App;

import java.time.format.DateTimeFormatter;

public abstract class BaseTable {
    protected static final DateTimeFormatter timestampFormatter = App.getAppContainer().getTimestampFormatter();

    protected SQLiteOpenHelper helper;
    protected String name;

    public BaseTable(SQLiteOpenHelper helper, String name) {
        this.helper = helper;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

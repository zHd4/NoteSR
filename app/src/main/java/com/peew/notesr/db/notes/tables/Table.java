package com.peew.notesr.db.notes.tables;

import android.database.sqlite.SQLiteOpenHelper;

import java.time.format.DateTimeFormatter;

public abstract class Table {
    protected static final DateTimeFormatter timestampFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    protected SQLiteOpenHelper helper;
    protected String name;

    public Table(SQLiteOpenHelper helper, String name) {
        this.helper = helper;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

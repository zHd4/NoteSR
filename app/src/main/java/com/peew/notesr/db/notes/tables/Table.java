package com.peew.notesr.db.notes.tables;

import android.database.sqlite.SQLiteOpenHelper;

public abstract class Table {
    protected SQLiteOpenHelper helper;
    protected String name;

    public Table(SQLiteOpenHelper helper, String name) {
        this.helper = helper;
        this.name = name;
    }
}

package com.peew.notesr.db.notes.tables;

import android.database.sqlite.SQLiteOpenHelper;

import java.util.Map;

public abstract class Table {
    protected SQLiteOpenHelper helper;
    public abstract String getName();
    public abstract Map<String, String> getFields();

    public Table(SQLiteOpenHelper helper) {
        this.helper = helper;
    }
}

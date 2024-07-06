package com.peew.notesr.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.peew.notesr.App;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseDB extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    protected final Map<Class<? extends BaseTable>, BaseTable> tables = new HashMap<>();

    public BaseDB(String name) {
        super(App.getContext(), name, null, DATABASE_VERSION);
    }

    public <T extends BaseTable> T getTable(Class<? extends BaseTable> tableClass) {
        return (T) tables.get(tableClass);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}

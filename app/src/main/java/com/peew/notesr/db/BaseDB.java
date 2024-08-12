package com.peew.notesr.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.peew.notesr.App;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseDB extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    protected final Map<Class<? extends BaseTable>, BaseTable> tables = new HashMap<>();

    public final Databases databases;

    public BaseDB(String name) {
        super(App.getContext(), name, null, DATABASE_VERSION);
        this.databases = new Databases(getReadableDatabase(), getWritableDatabase());
    }

    public <T extends BaseTable> T getTable(Class<? extends BaseTable> tableClass) {
        return (T) tables.get(tableClass);
    }

    public void beginTransaction() {
        databases.writable.beginTransaction();
    }

    public void rollbackTransaction() {
        databases.writable.endTransaction();
    }

    public void commitTransaction() {
        getWritableDatabase().setTransactionSuccessful();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {}

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public static class Databases {
        public final SQLiteDatabase readable;
        public final SQLiteDatabase writable;

        Databases(SQLiteDatabase readable, SQLiteDatabase writable) {
            this.readable = readable;
            this.writable = writable;
        }
    }
}

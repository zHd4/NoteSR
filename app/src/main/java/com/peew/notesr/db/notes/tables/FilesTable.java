package com.peew.notesr.db.notes.tables;

import android.database.sqlite.SQLiteOpenHelper;

public class FilesTable extends Table{
    public FilesTable(SQLiteOpenHelper helper, String name) {
        super(helper, name);
        helper.getWritableDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id integer PRIMARY KEY AUTOINCREMENT, " +
                        "encrypted_name text NOT NULL, " +
                        "encrypted_data blob NOT NULL)");
    }
}

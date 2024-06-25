package com.peew.notesr.db.notes.tables;

import android.database.sqlite.SQLiteOpenHelper;

public class DataBlocksTable extends Table {
    public DataBlocksTable(SQLiteOpenHelper helper, String name, FilesTable filesTable) {
        super(helper, name);

        helper.getWritableDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        "id integer PRIMARY KEY AUTOINCREMENT, " +
                        "file_id integer NOT NULL, " +
                        "block_order bigint NOT NULL, " +
                        "data blob NOT NULL, " +
                        "FOREIGN KEY(file_id) REFERENCES " + filesTable.getName() + "(id))"
        );
    }
}

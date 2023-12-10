package com.peew.notesr.db.notes.tables;

import android.database.sqlite.SQLiteOpenHelper;

import java.util.Map;

public final class NotesTable extends Table {
    private final String name = "notes";
    private final Map<String, String> fields = Map.of(
            "note_id", "INTEGER",
            "encrypted_name", "TEXT",
            "encrypted_data", "TEXT"
            );

    public NotesTable(SQLiteOpenHelper helper) {
        super(helper);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getFields() {
        return fields;
    }
}
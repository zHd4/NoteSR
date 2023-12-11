package com.peew.notesr.db.notes.tables;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.db.notes.tables.models.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NotesTable extends Table {
    private final String name = "notes";
    private final Map<String, String> fields = Map.of(
            "note_id", "TEXT",
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

    public List<Note> getAllNotes() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Note> notes = new ArrayList<>();

        Cursor cursor = db.query(name,
                null,
                null,
                null,
                null,
                null,
                null);

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    long id = Long.parseLong(decrypt(cursor.getString(0)));

                    String name = decrypt(cursor.getString(1));
                    String text = decrypt(cursor.getString(2));

                    notes.add(new Note(id, name, text));
                } while (cursor.moveToNext());
            }
        }

        return notes;
    }
}
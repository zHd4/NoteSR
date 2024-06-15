package com.peew.notesr.db.notes.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.model.EncryptedNote;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class NotesTable extends Table {
    public NotesTable(SQLiteOpenHelper helper, String name) {
        super(helper, name);

        helper.getWritableDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                "note_id integer PRIMARY KEY AUTOINCREMENT, " +
                "encrypted_name text NOT NULL, " +
                "encrypted_data text NOT NULL, " +
                "updated_at varchar(255) NOT NULL)"
        );
    }

    public void save(EncryptedNote note) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("encrypted_name", note.getEncryptedName());
        values.put("encrypted_data", note.getEncryptedText());
        values.put("updated_at", LocalDateTime.now().format(timestampFormatter));

        if (note.getId() == null || get(note.getId()) == null) {
            long id = db.insert(name, null, values);
            note.setId(id);
        } else {
            db.update(name, values, "note_id = ?" ,
                    new String[] { String.valueOf(note.getId()) });
        }
    }

    public List<EncryptedNote> getAll() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<EncryptedNote> notes = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT * FROM " + name + " ORDER BY updated_at DESC",
                new String[0]);

        try (cursor) {
            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(0);

                    String name = cursor.getString(1);
                    String text = cursor.getString(2);

                    String updatedAtStr = cursor.getString(3);
                    LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr, timestampFormatter);

                    EncryptedNote note = new EncryptedNote(name, text, updatedAt);

                    note.setId(id);
                    notes.add(note);
                } while (cursor.moveToNext());
            }
        }

        return notes;
    }

    public EncryptedNote get(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT encrypted_name, encrypted_data, updated_at" +
                        " FROM " + name + " WHERE note_id = ?",
                new String[] { String.valueOf(id) });

        try (cursor) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String text = cursor.getString(1);

                String updatedAtStr = cursor.getString(2);
                LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr, timestampFormatter);

                EncryptedNote note = new EncryptedNote(name, text, updatedAt);

                note.setId(id);
                return note;
            }
        }

        return null;
    }

    public void delete(long id) {
        helper.getWritableDatabase()
                .delete(name, "note_id" + "=" + id, null);
    }
}

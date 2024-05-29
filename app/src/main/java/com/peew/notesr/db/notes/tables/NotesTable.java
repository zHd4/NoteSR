package com.peew.notesr.db.notes.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.model.EncryptedNote;

import java.util.ArrayList;
import java.util.List;

public final class NotesTable extends Table {
    public NotesTable(SQLiteOpenHelper helper, String name) {
        super(helper, name);
        helper.getWritableDatabase().execSQL(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                "note_id integer PRIMARY KEY AUTOINCREMENT, " +
                "encrypted_name text NOT NULL, " +
                "encrypted_data text NOT NULL)");
    }

    public void save(EncryptedNote note) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("encrypted_name", note.getEncryptedName());
        values.put("encrypted_data", note.getEncryptedText());

        if (note.getId() == null || !exists(note.getId())) {
            db.insert(name, null, values);
        } else {
            db.update(name, values, "note_id" + "=" + note.getId(), null);
        }
    }

    public boolean exists(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        boolean exists;

        Cursor cursor = db.query(name,
                new String[] { "note_id" },
                "note_id" + "=" + id,
                null,
                null,
                null,
                null);

        try (cursor){
            exists = cursor.moveToFirst();
        }

        return exists;
    }

    public List<EncryptedNote> getAll() {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<EncryptedNote> notes = new ArrayList<>();

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
                    long id = cursor.getLong(0);

                    String name = cursor.getString(1);
                    String text = cursor.getString(2);

                    EncryptedNote note = new EncryptedNote(name, text);

                    note.setId(id);
                    notes.add(note);
                } while (cursor.moveToNext());
            }
        }

        return notes;
    }

    public EncryptedNote get(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(name,
                null,
                "note_id" + "=" + id,
                null,
                null,
                null,
                null);

        try (cursor) {
            if (cursor.moveToFirst()) {
                String name = cursor.getString(1);
                String text = cursor.getString(2);

                EncryptedNote note = new EncryptedNote(name, text);

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

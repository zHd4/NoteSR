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
        helper.getWritableDatabase().execSQL("CREATE TABLE IF NOT EXISTS " + name + "(" +
                "note_id bigint PRIMARY KEY AUTOINCREMENT," +
                "encrypted_name text NOT NULL," +
                "encrypted_data text NOT NULL)");
    }

    public void add(EncryptedNote note) {
        try (SQLiteDatabase db = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put("note_id", note.id());

            values.put("encrypted_name", note.encryptedName());
            values.put("encrypted_data", note.encryptedText());

            db.insert(name, null, values);
        }
    }

    public boolean exists(long id) {
        boolean exists;

        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(name,
                new String[] { "note_id" },
                "note_id" + "=" + id,
                null,
                null,
                null,
                null);

        try (db) {
            try (cursor){
                exists = cursor.moveToFirst();
            }
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

        try (db) {
            try (cursor) {
                if (cursor.moveToFirst()) {
                    do {
                        long id = cursor.getLong(0);

                        String name = cursor.getString(1);
                        String text = cursor.getString(2);

                        notes.add(new EncryptedNote(id, name, text));
                    } while (cursor.moveToNext());
                }
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

        try (db) {
            try (cursor) {
                if (cursor.moveToFirst()) {
                    String name = cursor.getString(1);
                    String text = cursor.getString(2);

                    return new EncryptedNote(id, name, text);
                } else {
                    throw new RuntimeException("Wrong note id");
                }
            }
        }
    }

    public long getNewNoteId() {
        long newId = 0;

        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.query(name,
                null,
                null,
                null,
                null,
                null,
                "note_id DESC",
                "1");

        try (db) {
            try (cursor) {
                if (cursor.moveToFirst()) {
                    newId = cursor.getLong(0) + 1;
                }
            }
        }

        return newId;
    }

    public void update(EncryptedNote note) {
        try (SQLiteDatabase db = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();

            values.put("encrypted_name", note.encryptedName());
            values.put("encrypted_data", note.encryptedText());

            db.update(name, values, "note_id" + "=" + note.id(), null);
        }
    }

    public void delete(long id) {
        try (SQLiteDatabase db = helper.getWritableDatabase()) {
            db.delete(name, "note_id" + "=" + id, null);
        }
    }
}

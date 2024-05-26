package com.peew.notesr.db.notes.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.model.EncryptedNote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NotesTable extends Table {
    private final String name = "notes";
    private final Map<String, String> fields = Map.of(
            NotesTableField.NOTE_ID.getName(), "BIGINT",
            NotesTableField.ENCRYPTED_NAME.getName(), "TEXT",
            NotesTableField.ENCRYPTED_DATA.getName(), "TEXT"
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

    @Deprecated
    @Override
    public void reEncryptAll(CryptoKey oldCryptoKey) {
        SQLiteDatabase db = helper.getWritableDatabase();
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

                        String name = decrypt(cursor.getString(1), oldCryptoKey);
                        String text = decrypt(cursor.getString(2), oldCryptoKey);

                        String encryptedName = encrypt(name);
                        String encryptedText = encrypt(text);

                        ContentValues values = new ContentValues();

                        values.put(NotesTableField.ENCRYPTED_NAME.getName(), encryptedName);
                        values.put(NotesTableField.ENCRYPTED_DATA.getName(), encryptedText);

                        String whereClause = NotesTableField.NOTE_ID.getName() + "=" + id;
                        db.update(this.name, values, whereClause, null);
                    } while (cursor.moveToNext());
                }
            }
        }
    }

    public void add(EncryptedNote note) {
        try (SQLiteDatabase db = helper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(NotesTableField.NOTE_ID.getName(), note.id());

            values.put(NotesTableField.ENCRYPTED_NAME.getName(), note.encryptedName());
            values.put(NotesTableField.ENCRYPTED_DATA.getName(), note.encryptedText());

            db.insert(name, null, values);
        }
    }

    public boolean exists(long id) {
        boolean exists;

        SQLiteDatabase db = helper.getReadableDatabase();
        String idFieldName = NotesTableField.NOTE_ID.getName();

        Cursor cursor = db.query(name,
                new String[] { idFieldName },
                idFieldName + "=" + id,
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
        String idFieldName = NotesTableField.NOTE_ID.getName();

        Cursor cursor = db.query(name,
                null,
                idFieldName + "=" + id,
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
        String idFieldName = NotesTableField.NOTE_ID.getName();

        Cursor cursor = db.query(name,
                null,
                null,
                null,
                null,
                null,
                idFieldName + " DESC",
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

            values.put(NotesTableField.ENCRYPTED_NAME.getName(), note.encryptedName());
            values.put(NotesTableField.ENCRYPTED_DATA.getName(), note.encryptedText());

            String whereClause = NotesTableField.NOTE_ID.getName() + "=" + note.id();
            db.update(name, values, whereClause, null);
        }
    }

    public void delete(long id) {
        try (SQLiteDatabase db = helper.getWritableDatabase()) {
            String idFieldName = NotesTableField.NOTE_ID.name();
            db.delete(name, idFieldName + "=" + id, null);
        }
    }

    private enum NotesTableField {
        NOTE_ID("note_id"),
        ENCRYPTED_NAME("encrypted_name"),
        ENCRYPTED_DATA("encrypted_data");

        private final String name;

        NotesTableField(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
